package piuk.blockchain.android.ui.kyc.tiercurrentstate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.awaitOutcome
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyActivity
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycEntryPoint
import timber.log.Timber

class TierCurrentStateFragment : Fragment() {

    private val progressListener: KycProgressListener by ParentActivityDelegate(this)
    private val kycService: KycService by scopedInject()
    private val analytics: Analytics by inject()

    private val kycState: KycState by lazy {
        TierCurrentStateFragmentArgs.fromBundle(requireArguments()).kycState
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            TierCurrentStateScreen(
                state = kycState,
                underReviewCtaClicked = ::finish,
                verifiedCtaClicked = ::finish,
                rejectedCtaClicked = ::finish
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.hideBackButton()
        progressListener.setupHostToolbar(
            title = null,
            navigationBarButtons = listOf(
                NavigationBarButton.Icon(
                    drawable = com.blockchain.componentlib.R.drawable.ic_close_circle,
                    color = null,
                    contentDescription = com.blockchain.stringResources.R.string.accessibility_close,
                    onIconClick = ::finish
                )
            )
        )

        lifecycleScope.launchWhenCreated {
            kycService.getTiersLegacy().awaitOutcome()
                .doOnSuccess { tiers ->
                    val silverKycState = tiers.tierForLevel(KycTier.SILVER).state
                    val goldKycState = tiers.tierForLevel(KycTier.GOLD).state

                    val pendingOrApproved = listOf(KycTierState.Pending, KycTierState.Verified)
                    when {
                        goldKycState in pendingOrApproved -> analytics.logEvent(AnalyticsEvents.KycTier2Complete)
                        silverKycState in pendingOrApproved -> analytics.logEvent(AnalyticsEvents.KycTier1Complete)
                        silverKycState == KycTierState.None -> analytics.logEvent(AnalyticsEvents.KycTiersLocked)
                    }
                }
                .doOnFailure(Timber::e)
        }
    }

    private fun finish() {
        when (progressListener.entryPoint) {
            KycEntryPoint.Buy -> {
                activity?.setResult(SimpleBuyActivity.RESULT_KYC_SIMPLE_BUY_COMPLETE)
                activity?.finish()
            }
            else -> {
                activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
                activity?.finish()
            }
        }
    }
}
