package piuk.blockchain.android.ui.maintenance.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
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
            statusUiSettings = AppMaintenanceStatusUiSettings.fromStatus(state.status)
        )
    }

    override suspend fun handleIntent(modelState: AppMaintenanceModelState, intent: AppMaintenanceIntents) {
        when (intent) {
            AppMaintenanceIntents.RedirectToWebsite -> {
                require(modelState.status is AppMaintenanceStatus.Actionable.RedirectToWebsite)
                { "Intent RedirectToWebsite called with incorrect status ${modelState.status}" }

                navigate(AppMaintenanceNavigationEvent.RedirectToWebsite(modelState.status.website))
            }

            AppMaintenanceIntents.ViewStatus -> {
                require(modelState.status is AppMaintenanceStatus.Actionable.SiteWideMaintenance)
                { "Intent ViewStatus called with incorrect status ${modelState.status}" }

                navigate(AppMaintenanceNavigationEvent.RedirectToWebsite(modelState.status.statusUrl))
            }

            AppMaintenanceIntents.SkipUpdate -> {
                require(modelState.status is AppMaintenanceStatus.Actionable.OptionalUpdate)
                { "Intent SkipUpdate called with incorrect status ${modelState.status}" }


            }



            else -> {
            }
        }.exhaustive
    }

    private fun getAppMaintenanceStatus() {
        viewModelScope.launch {
            getAppMaintenanceConfigUseCase().let { status ->
                when (status) {
                    AppMaintenanceStatus.NonActionable.Unknown -> {
                        // todo what
                    }

                    AppMaintenanceStatus.NonActionable.AllClear -> {
                        // todo close
                    }

                    is AppMaintenanceStatus.Actionable -> {
                        updateState { it.copy(status = status) }
                    }
                }
            }
        }
    }
}

