package piuk.blockchain.android.ui.maintenance.presentation

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel

class AppMaintenanceViewModel(
    AppMaintenanceModelState: AppMaintenanceModelState,
) : MviViewModel<AppMaintenanceIntents, AppMaintenanceViewState, AppMaintenanceModelState, AppMaintenanceNavigationEvent, ModelConfigArgs.NoArgs>(
    AppMaintenanceModelState
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AppMaintenanceModelState): AppMaintenanceViewState {
        return AppMaintenanceViewState(
        )
    }

    override suspend fun handleIntent(modelState: AppMaintenanceModelState, intent: AppMaintenanceIntents) {
    }
}

