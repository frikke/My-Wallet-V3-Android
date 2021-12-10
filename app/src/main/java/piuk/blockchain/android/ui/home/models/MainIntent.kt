package piuk.blockchain.android.ui.home.models

import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class MainIntent : MviIntent<MainState> {
    object PerformInitialChecks : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
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

    object LaunchExchange : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object CancelAnyPendingConfirmationBuy : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }

    object ResetViewState : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState.copy(
            viewToLaunch = ViewToLaunch.None
        )
    }

    class ProcessScanResult(val decodedData: String) : MainIntent() {
        override fun reduce(oldState: MainState): MainState = oldState
    }
}
