package piuk.blockchain.android.ui.kyc.veriffsplash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.commonarch.presentation.mvi_v2.MVIFragment
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.unsafeLazy
import com.blockchain.veriff.VeriffLauncher
import com.blockchain.veriff.VeriffResultHandler
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.core.scope.Scope
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener

class VeriffSplashFragment :
    MVIFragment<VeriffSplashViewState>(),
    NavigationRouter<Navigation>,
    AndroidScopeComponent {

    override var scope: Scope? = payloadScope

    private val model: VeriffSplashModel by scopedInject()
    private val analytics: Analytics by inject()
    private val fraudService: FraudService by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)

    private val veriffResultHandler = VeriffResultHandler(
        onSuccess = {
            model.onIntent(VeriffSplashIntent.OnVeriffSuccess)
            fraudService.endFlow(FraudFlow.KYC)
        },
        onError = {
            model.onIntent(VeriffSplashIntent.OnVeriffFailure(it))
            fraudService.endFlow(FraudFlow.KYC)
        }
    )

    private val countryCode by unsafeLazy {
        VeriffSplashFragmentArgs.fromBundle(arguments ?: Bundle()).countryCode
    }

    private var progressDialog: MaterialProgressDialog? = null
    private var ctaClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            VeriffSplashScreen(
                viewState = model.viewState,
                onIntent = model::onIntent,
                nextClicked = { ctaClicked = true }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setupHostToolbar(R.string.kyc_veriff_splash_title)
        analytics.logEvent(AnalyticsEvents.KycVerifyIdentity)
        analytics.logEvent(KYCAnalyticsEvents.MoreInfoViewed)
        fraudService.trackFlow(FraudFlow.KYC)

        bindViewModel(model, this, Args(countryCode))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!ctaClicked) analytics.logEvent(KYCAnalyticsEvents.MoreInfoDismissed)
    }

    override fun onStateUpdated(state: VeriffSplashViewState) {
        if (state.isLoading) showProgressDialog()
        else dismissProgressDialog()
    }

    override fun route(navigationEvent: Navigation) {
        when (navigationEvent) {
            is Navigation.TierCurrentState -> {
                fraudService.endFlow(FraudFlow.KYC)

                findNavController(this).navigate(
                    KycNavXmlDirections.actionStartTierCurrentState(
                        navigationEvent.kycState,
                        navigationEvent.isSddVerified,
                    )
                )
            }
            is Navigation.Veriff -> {
                VeriffLauncher().launchVeriff(
                    requireActivity(),
                    navigationEvent.veriffApplicantAndToken,
                    REQUEST_CODE_VERIFF
                )
                ctaClicked = true
                analytics.logEvent(KYCAnalyticsEvents.MoreInfoCtaClicked)
                logEvent(KYCAnalyticsEvents.VeriffInfoStarted)
            }
        }
    }

    private fun showProgressDialog() {
        if (progressDialog != null) return
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setMessage(R.string.kyc_country_selection_please_wait)
            setCancelable(false)
            show()
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VERIFF) {
            if (data != null) {
                veriffResultHandler.handleResult(data)
            } else if (resultCode == RESULT_OK) {
                model.onIntent(VeriffSplashIntent.OnVeriffSuccess)
                fraudService.endFlow(FraudFlow.KYC)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val REQUEST_CODE_VERIFF = 1440
    }
}
