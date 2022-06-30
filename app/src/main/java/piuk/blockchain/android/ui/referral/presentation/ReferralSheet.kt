package piuk.blockchain.android.ui.referral.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.blockchain.analytics.Analytics
import com.blockchain.commonarch.presentation.mvi_v2.MVIBottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.NavigationRouter
import com.blockchain.commonarch.presentation.mvi_v2.bindViewModel
import com.blockchain.commonarch.presentation.mvi_v2.disableDragging
import com.blockchain.commonarch.presentation.mvi_v2.withArgs
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.koin.payloadScope
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent.get
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.referral.presentation.composable.ReferralScreen
import piuk.blockchain.android.util.copyToClipboard
import piuk.blockchain.android.util.shareTextWithSubject

class ReferralSheet :
    MVIBottomSheet<ReferralViewState>(),
    NavigationRouter<ReferralNavigationEvent>,
    Analytics by get(Analytics::class.java),
    AndroidScopeComponent {

    private val args: ReferralArgs by lazy {
        arguments?.getParcelable<ReferralArgs>(ReferralArgs.ARGS_KEY) ?: error("missing ReferralArgs")
    }

    override val scope: Scope = payloadScope

    private val viewModel: ReferralViewModel by viewModel()

    private val analytics: Analytics by inject()

    override fun onStateUpdated(state: ReferralViewState) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        disableDragging()

        setupViewModel()
        analytics.logEvent(ReferralAnalyticsEvents.ReferralView(args.campaignId))

        return ComposeView(requireContext()).apply {
            setContent {
                SheetContent()
            }
        }
    }

    private fun setupViewModel() {
        bindViewModel(viewModel = viewModel, navigator = this, args = args)
    }

    @Composable
    private fun SheetContent() {
        with(viewModel.viewState.collectAsState().value) {
            ReferralScreen(
                rewardTitle = rewardTitle,
                rewardSubtitle = rewardSubtitle,
                code = code,
                confirmCopiedToClipboard = confirmCopiedToClipboard,
                criteria = criteria,
                onBackPressed = ::onBackPressed,
                copyToClipboard = ::copyToClipboard,
                shareCode = ::shareCode
            )
        }
    }

    private fun onBackPressed() {
        dismiss()
    }

    private fun copyToClipboard(code: String) {
        analytics.logEvent(ReferralAnalyticsEvents.ReferralCopyCode(code, args.campaignId))
        context?.copyToClipboard(CLIPBOARD_LABEL, code)
        viewModel.onIntent(ReferralIntents.ConfirmCopiedToClipboard)
    }

    private fun shareCode(code: String) {
        analytics.logEvent(ReferralAnalyticsEvents.ReferralShareCode(code, args.campaignId))
        context?.shareTextWithSubject(
            text = getString(R.string.referral_share_template, code),
            subject = getString(R.string.referral_share_template_subject)
        )
    }

    override fun route(navigationEvent: ReferralNavigationEvent) {
    }

    companion object {
        private const val CLIPBOARD_LABEL = "referralCode"

        fun newInstance(referralData: ReferralInfo.Data) = ReferralSheet().withArgs(
            key = ReferralArgs.ARGS_KEY,
            args = referralData.mapArgs()
        )
    }
}
