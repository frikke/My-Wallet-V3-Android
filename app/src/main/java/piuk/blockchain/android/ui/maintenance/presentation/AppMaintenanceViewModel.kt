package piuk.blockchain.android.ui.maintenance.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.ui.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase

class AppMaintenanceViewModel(
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase
) : MviViewModel<AppMaintenanceIntents,
    AppMaintenanceViewState,
    AppMaintenanceModelState,
    AppMaintenanceNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = AppMaintenanceModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        getAppMaintenanceStatus()
    }

    override fun reduce(state: AppMaintenanceModelState): AppMaintenanceViewState {
        return AppMaintenanceViewState(
            image = state.appMaintenanceStatusUi.image,
            title = state.appMaintenanceStatusUi.title,
            description = state.appMaintenanceStatusUi.description,
            button1Text = state.appMaintenanceStatusUi.button1Text,
            button2Text = state.appMaintenanceStatusUi.button2Text
        )
    }

    override suspend fun handleIntent(modelState: AppMaintenanceModelState, intent: AppMaintenanceIntents) {
    }

    private fun getAppMaintenanceStatus() {
        viewModelScope.launch {
            getAppMaintenanceConfigUseCase().let { status ->
                when (status) {
                    AppMaintenanceStatus.NonActionable.Unknown -> { // todo what
                    }

                    AppMaintenanceStatus.NonActionable.AllClear -> {
                        // todo close
                    }

                    is AppMaintenanceStatus.Actionable -> {
                        updateState { it.copy(appMaintenanceStatusUi = AppMaintenanceStatusUi.fromStatus(status)) }
                    }
                }
            }
        }
    }
}

