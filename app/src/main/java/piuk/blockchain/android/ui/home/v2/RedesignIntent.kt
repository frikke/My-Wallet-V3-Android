package piuk.blockchain.android.ui.home.v2

import android.content.Intent
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class RedesignIntent : MviIntent<RedesignState> {
    object PerformInitialChecks : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    class CheckForPendingLinks(val appIntent: Intent) : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    class UpdateViewToLaunch(private val nextState: ViewToLaunch) : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState =
            oldState.copy(
                viewToLaunch = nextState
            )
    }

    class ValidateAccountAction(val action: AssetAction, val account: BlockchainAccount) : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    object UnpairWallet : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    object LaunchExchange : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    object CancelAnyPendingConfirmationBuy : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }

    class ProcessScanResult(val decodedData: String) : RedesignIntent() {
        override fun reduce(oldState: RedesignState): RedesignState = oldState
    }
}
