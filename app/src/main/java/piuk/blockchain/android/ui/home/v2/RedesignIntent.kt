package piuk.blockchain.android.ui.home.v2

import android.content.Intent
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
}
