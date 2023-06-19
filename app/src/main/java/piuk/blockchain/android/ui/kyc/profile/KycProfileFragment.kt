package piuk.blockchain.android.ui.kyc.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.koin.payloadScope
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.util.Calendar
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate

class KycProfileFragment : MVIFragment<KycProfileViewState>(), NavigationRouter<Navigation>, AndroidScopeComponent {

    override val scope: Scope = payloadScope

    private val viewModel: KycProfileModel by viewModel()
    private val analytics: Analytics by inject()
    private val fraudService: FraudService by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    val countryCode: CountryIso by lazy {
        KycProfileFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).countryCode
    }

    val stateCode: StateIso? by lazy {
        KycProfileFragmentArgs.fromBundle(
            arguments ?: Bundle()
        ).stateCode.takeIf { it.isNotEmpty() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ComposeView(requireContext()).apply {
            setContent {
                KycProfileScreen(
                    viewState = viewModel.viewState,
                    onIntent = viewModel::onIntent,
                    isSavingProfileLoadingOverride = false,
                    showDatePicker = ::showDatePicker
                )
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isCowboysUser = (requireActivity() as? KycNavHostActivity)?.isCowboysUser == true
        bindViewModel(
            viewModel,
            this,
            Args(countryCode, stateCode, isCowboysUser)
        )

        logEvent(AnalyticsEvents.KycProfile)
        fraudService.trackFlow(FraudFlow.ONBOARDING)

        progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_profile_title)

        if (isCowboysUser) {
            analytics.logEvent(CowboysAnalytics.KycPersonalInfoViewed)
        }
    }

    override fun onStateUpdated(state: KycProfileViewState) {
        // no-op
    }

    override fun route(navigationEvent: Navigation) {
        when (navigationEvent) {
            is Navigation.AddressVerification -> navigate(
                KycProfileFragmentDirections.actionKycProfileFragmentToKycAddressVerificationFragment(
                    navigationEvent.profileModel
                )
            )
        }
    }

    private fun showDatePicker() {
        (requireActivity() as? AppCompatActivity)?.hideKeyboard()

        val calendar = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }
        DatePickerDialog.newInstance(
            datePickerCallback,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(requireContext().getString(com.blockchain.stringResources.R.string.kyc_profile_dob_hint))
            maxDate = calendar
            showYearPickerFirst(true)
            show(requireActivity().fragmentManager, tag)
        }
    }

    private val datePickerCallback: DatePickerDialog.OnDateSetListener by lazy {
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            val dateOfBirth = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            viewModel.onIntent(KycProfileIntent.DateOfBirthInputChanged(dateOfBirth))
        }
    }
}
