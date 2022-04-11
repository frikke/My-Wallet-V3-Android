package piuk.blockchain.android.ui.maintenance.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import piuk.blockchain.android.ui.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.ui.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.ui.maintenance.domain.usecase.IsDownloadInProgressUseCase
import piuk.blockchain.android.ui.maintenance.domain.usecase.SkipAppUpdateUseCase

class AppMaintenanceViewModel(
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase,
    private val isDownloadInProgressUseCase: IsDownloadInProgressUseCase,
    private val skipAppUpdateUseCase: SkipAppUpdateUseCase
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

                navigate(AppMaintenanceNavigationEvent.OpenUrl(modelState.status.website))
            }

            AppMaintenanceIntents.ViewStatus -> {
                require(modelState.status is AppMaintenanceStatus.Actionable.SiteWideMaintenance)
                { "Intent ViewStatus called with incorrect status ${modelState.status}" }

                navigate(AppMaintenanceNavigationEvent.OpenUrl(modelState.status.statusUrl))
            }

            AppMaintenanceIntents.SkipUpdate -> {
                require(modelState.status is AppMaintenanceStatus.Actionable.OptionalUpdate)
                { "Intent SkipUpdate called with incorrect status ${modelState.status}" }

                skipUpdateVersion(modelState.status.softVersionCode)
                navigate(AppMaintenanceNavigationEvent.ResumeAppFlow)
            }

            AppMaintenanceIntents.UpdateApp -> {
                require(
                    modelState.status is AppMaintenanceStatus.Actionable.OptionalUpdate
                        || modelState.status is AppMaintenanceStatus.Actionable.MandatoryUpdate
                )
                { "Intent UpdateApp called with incorrect status ${modelState.status}" }

                navigate(AppMaintenanceNavigationEvent.LaunchAppUpdate)
            }
        }.exhaustive
    }

    private fun getAppMaintenanceStatus() {
        viewModelScope.launch {
            if (isDownloadInProgressUseCase()) navigate(AppMaintenanceNavigationEvent.LaunchAppUpdate)

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

    /**
     * mark [versionCode] as skipped
     */
    private fun skipUpdateVersion(versionCode: Int) {
        skipAppUpdateUseCase(versionCode)
    }
}

