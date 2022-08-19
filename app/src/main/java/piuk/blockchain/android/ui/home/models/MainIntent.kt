package piuk.blockchain.android.ui.home.models

import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.componentlib.navigation.NavigationItem
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletmode.WalletMode

sealed class MainIntent : MviIntent<MainState> {
    data class PerformInitialChecks(val deeplinkIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object CheckReferralCode : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object NavigationTabs : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class RefreshTabs(val walletMode: WalletMode) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy(walletMode = walletMode)
    }

    class ReferralCodeIntent(private val referralState: ReferralState) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                referral = referralState
            )
    }

    class UpdateTabs(private val tabs: List<NavigationItem>, private val selectedTab: NavigationItem) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                currentTab = selectedTab,
                tabs = tabs
            )
    }

    class UpdateCurrentTab(private val item: NavigationItem) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(currentTab = item)
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

    class UpdateViewToLaunch(private val nextState: ViewToLaunch) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                viewToLaunch = nextState
            )
    }

    class ValidateAccountAction(val action: AssetAction, val account: BlockchainAccount?) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
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

    class SwitchWalletMode(val walletMode: WalletMode) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class UpdateDeepLinkResult(val deeplinkResult: DeepLinkResult) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = deeplinkResult
            )
    }

    object ClearDeepLinkResult : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = DeepLinkResult.DeepLinkResultFailed()
            )
    }

    data class ProcessPendingDeeplinkIntent(val deeplinkIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }
}
