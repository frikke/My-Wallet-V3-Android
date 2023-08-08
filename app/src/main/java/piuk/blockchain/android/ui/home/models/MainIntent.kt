package piuk.blockchain.android.ui.home.models

import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.walletconnect.domain.WalletConnectSession

sealed class MainIntent : MviIntent<MainState> {
    data class PerformInitialChecks(val deeplinkIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object CheckReferralCode : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class ReferralCodeIntent(private val referralState: ReferralState) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                referral = referralState
            )
    }

    object ReferralIconClicked : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy(
            referral = oldState.referral.copy(hasReferralBeenClicked = true)
        )
    }

    object ShowReferralWhenAvailable : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(referral = ReferralState(ReferralInfo.NotAvailable, referralDeeplink = true))
    }

    class CheckForPendingLinks(val appIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class UpdateViewToLaunch(private val view: ViewToLaunch) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                viewToLaunch = view
            )
    }

    object UnpairWallet : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object ResetViewState : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy(
            viewToLaunch = ViewToLaunch.None
        )
    }

    class RejectWCSession(val session: WalletConnectSession) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class ProcessScanResult(val decodedData: String) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class ApproveWCSession(val session: WalletConnectSession) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class StartWCSession(val url: String) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class SelectNetworkForWCSession(val session: WalletConnectSession) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class GetNetworkInfoForWCSession(val session: WalletConnectSession) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class UpdateDeepLinkResult(private val deeplinkResult: DeepLinkResult) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = deeplinkResult
            )
    }

    object ClearDeepLinkResult : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = DeepLinkResult.DeepLinkResultUnknownLink()
            )
    }

    data class ProcessPendingDeeplinkIntent(val deeplinkIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object LoadFeatureFlags : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy()
    }

    class UpdateFlags(
        private val isEarnEnabled: Boolean
    ) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(isEarnOnNavEnabled = isEarnEnabled)
    }

    class LaunchTransactionFlowFromDeepLink(val networkTicker: String, val action: AssetAction) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class SelectRewardsAccountForAsset(val cryptoTicker: String) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }
}
