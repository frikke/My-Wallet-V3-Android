package piuk.blockchain.android.ui.kyc.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.KYCAnalyticsEvents
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.koin.android.ext.android.inject
import piuk.blockchain.android.databinding.FragmentKycSplashBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import piuk.blockchain.android.ui.kyc.navigate

class KycSplashFragment : BaseFragment<KycSplashView, KycSplashPresenter>(), KycSplashView {

    private var _binding: FragmentKycSplashBinding? = null
    private val binding: FragmentKycSplashBinding
        get() = _binding!!

    private val presenter: KycSplashPresenter by scopedInject()

    private val settingsDataManager: SettingsDataManager by scopedInject()

    private val analytics: Analytics by inject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val entryPoint = progressListener.entryPoint
        logEvent(
            when (entryPoint) {
                KycEntryPoint.Other,
                KycEntryPoint.Swap -> AnalyticsEvents.KycWelcome
                KycEntryPoint.Resubmission -> AnalyticsEvents.KycResubmission
                KycEntryPoint.Buy -> AnalyticsEvents.KycSimpleBuyStart
                KycEntryPoint.FiatFunds -> AnalyticsEvents.KycFiatFundsStart
                KycEntryPoint.Interest -> AnalyticsEvents.KycFiatFundsStart
                else -> AnalyticsEvents.KycWelcome
            }
        )

        val title = when (entryPoint) {
            KycEntryPoint.Buy,
            KycEntryPoint.Resubmission,
            KycEntryPoint.FiatFunds -> com.blockchain.stringResources.R.string.buy_sell_splash_title
            KycEntryPoint.Swap -> com.blockchain.stringResources.R.string.kyc_splash_title
            KycEntryPoint.Interest -> com.blockchain.stringResources.R.string.earn_rewards
            KycEntryPoint.Other -> com.blockchain.stringResources.R.string.identity_verification
            else -> com.blockchain.stringResources.R.string.buy_sell_splash_title
        }

        progressListener.setupHostToolbar(title)

        with(binding) {
            textViewKycTermsAndConditions.gone()

            buttonKycSplashApplyNow.apply {
                text = getString(com.blockchain.stringResources.R.string.kyc_splash_apply_now)
                onClick = {
                    analytics.logEvent(KYCAnalyticsEvents.VerifyIdentityStart)
                    presenter.onCTATapped()
                }
            }
        }
    }

    private val disposable = CompositeDisposable()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun goToNextKycStep(direction: NavDirections) =
        navigate(direction)

    override fun displayLoading(isLoading: Boolean) {
        progressDialog = if (isLoading) {
            MaterialProgressDialog(requireContext()).apply {
                setMessage(com.blockchain.stringResources.R.string.buy_sell_please_wait)
                show()
            }
        } else {
            progressDialog?.apply { dismiss() }
            null
        }
    }

    override fun showError(message: String) =
        BlockchainSnackbar.make(
            binding.root,
            message,
            type = SnackbarType.Error
        ).show()

    override fun createPresenter(): KycSplashPresenter = presenter

    override fun getMvpView(): KycSplashView = this
}
