package piuk.blockchain.android.ui.kyc.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.addressverification.ui.AddressVerificationHost
import com.blockchain.addressverification.ui.AddressVerificationSavingError
import com.blockchain.addressverification.ui.AddressVerificationScreen
import com.blockchain.addressverification.ui.Args
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.support.SupportCentreActivity
import piuk.blockchain.android.ui.base.BaseMvpFragment
import piuk.blockchain.android.ui.cowboys.CowboysAnalytics
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel

class KycAddressVerificationFragment :
    BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView {

    private val addressVerificationHost = object : AddressVerificationHost() {
        override val errorWhileSaving = MutableSharedFlow<AddressVerificationSavingError>()

        override fun launchContactSupport() {
            fraudService.endFlow(FraudFlow.ONBOARDING)
            startActivity(SupportCentreActivity.newIntent(requireContext()))
        }

        override fun addressVerifiedSuccessfully(address: AddressDetails) {
            if ((requireActivity() as? KycNavHostActivity)?.isCowboysUser == true) {
                analytics.logEvent(CowboysAnalytics.KycAddressConfirmed)
            }

            fraudService.endFlow(FraudFlow.ONBOARDING)

            presenter.onContinueClicked(address)
            analytics.logEvent(KYCAnalyticsEvents.AddressChanged)
        }
    }

    override val profileModel: ProfileModel by unsafeLazy {
        KycAddressVerificationFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }

    private val presenter: KycHomeAddressPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val fraudService: FraudService by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireActivity()).apply {
            setContent {
                AddressVerificationScreen(
                    args = Args(
                        countryIso = profileModel.countryCode,
                        stateIso = profileModel.stateCode,
                        prefilledAddress = null,
                        allowManualOverride = true
                    ),
                    isVerifyAddressLoadingOverride = false,
                    host = addressVerificationHost
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logEvent(AnalyticsEvents.KycAddress)
        fraudService.trackFlow(FraudFlow.ONBOARDING)

        progressListener.setupHostToolbar(com.blockchain.stringResources.R.string.kyc_address_title)
    }

    override fun showErrorWhileSaving(error: AddressVerificationSavingError) {
        lifecycleScope.launch {
            addressVerificationHost.errorWhileSaving.emit(error)
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(com.blockchain.stringResources.R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun continueToVeriffSplash(countryCode: String) {
        fraudService.endFlow(FraudFlow.ONBOARDING)

        requireActivity().hideKeyboard()
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun continueToTier2MoreInfoNeeded(countryCode: String) {
        fraudService.endFlow(FraudFlow.ONBOARDING)

        requireActivity().hideKeyboard()
        navigate(KycNavXmlDirections.actionStartTier2NeedMoreInfo(countryCode))
    }

    override fun continueToQuestionnaire(questionnaire: Questionnaire, countryCode: String) {
        fraudService.endFlow(FraudFlow.ONBOARDING)

        requireActivity().hideKeyboard()
        navigate(
            KycAddressVerificationFragmentDirections.actionKycAddressVerificationFragmentToKycQuestionnaireFragment(
                questionnaire,
                countryCode
            )
        )
    }

    // TODO(aromano): KYC remove when I refactor NextStepDecision
    override fun tier1Complete() {
        requireActivity().hideKeyboard()
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
        activity?.finish()
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this
}
