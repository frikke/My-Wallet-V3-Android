package piuk.blockchain.android.ui.kyc.tiersplash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.blockchain.analytics.data.logEvent
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.databinding.FragmentKycTierSplashBinding
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener

class KycTierSplashFragment :
    BaseFragment<KycTierSplashView, KycTierSplashPresenter>(),
    KycTierSplashView {

    private val presenter: KycTierSplashPresenter by scopedInject()

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private var _binding: FragmentKycTierSplashBinding? = null
    private val binding get() = _binding!!
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycTierSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycTiers)

        val title = when (progressListener.campaignType) {
            CampaignType.Swap -> R.string.kyc_splash_title
            CampaignType.SimpleBuy,
            CampaignType.Resubmission,
            CampaignType.None,
            CampaignType.FiatFunds,
            CampaignType.Interest -> R.string.identity_verification
        }
        progressListener.setupHostToolbar(title)

        onViewReady()
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    override fun showError(@StringRes message: Int) =
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
}
