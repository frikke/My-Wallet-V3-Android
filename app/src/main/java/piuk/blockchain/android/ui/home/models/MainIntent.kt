package piuk.blockchain.android.ui.home.models

import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.deeplinking.processor.DeepLinkResult
import com.blockchain.walletconnect.domain.WalletConnectSession

sealed class MainIntent : MviIntent<MainState> {
    object PerformInitialChecks : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    data class CheckForInitialDialogs(val shouldStartUiTour: Boolean) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object CheckReferralCode : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    class ReferralCodeIntent(val referralInfo: ReferralState) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                referral = referralInfo
            )
    }

    object ReferralIconClicked : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy(
            referral = oldState.referral.copy(hasReferralBeenClicked = true)
        )
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

    class UpdateDeepLinkResult(val deeplinkResult: DeepLinkResult) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = deeplinkResult
            )
    }

    class SaveDeeplinkIntent(val deeplinkIntent: Intent) : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkIntent = deeplinkIntent
            )
    }

    object ClearDeepLinkResult : MainIntent() {
        override fun reduce(oldState: MainState): MainState =
            oldState.copy(
                deeplinkResult = DeepLinkResult.DeepLinkResultFailed
            )
    }
}
