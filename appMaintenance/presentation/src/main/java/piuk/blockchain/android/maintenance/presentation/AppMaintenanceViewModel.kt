package piuk.blockchain.android.maintenance.presentation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.extensions.exhaustive
import kotlinx.coroutines.launch
import piuk.blockchain.android.maintenance.domain.model.AppMaintenanceStatus
import piuk.blockchain.android.maintenance.domain.model.UpdateLocation
import piuk.blockchain.android.maintenance.domain.usecase.GetAppMaintenanceConfigUseCase
import piuk.blockchain.android.maintenance.domain.usecase.IsDownloadInProgressUseCase

class AppMaintenanceViewModel(
    private val getAppMaintenanceConfigUseCase: GetAppMaintenanceConfigUseCase,
    private val isDownloadInProgressUseCase: IsDownloadInProgressUseCase
) : MviViewModel<AppMaintenanceIntents,
    AppMaintenanceViewState,
    AppMaintenanceModelState,
    AppMaintenanceNavigationEvent,
    ModelConfigArgs.NoArgs>(
    initialState = AppMaintenanceModelState()
),
    DefaultLifecycleObserver {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        checkDownloadStatus()
    }

    /**
     * Get app maintenance status
     * Calling in [onResume] to update the status automatically when returning to the app
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        getAppMaintenanceStatus()
    }

    override fun reduce(state: AppMaintenanceModelState): AppMaintenanceViewState {
        return AppMaintenanceViewState(
            uiState = AppMaintenanceStatusUiState.fromStatus(state.status)
        )
    }

    override suspend fun handleIntent(modelState: AppMaintenanceModelState, intent: AppMaintenanceIntents) {
        when (intent) {
            AppMaintenanceIntents.RedirectToWebsite -> {
                require(
                    modelState.status is AppMaintenanceStatus.Actionable.RedirectToWebsite
                ) { "Intent RedirectToWebsite called with incorrect status ${modelState.status}" }

                openUrl(modelState.status.website)
            }

            AppMaintenanceIntents.ViewStatus -> {
                require(
                    modelState.status is AppMaintenanceStatus.Actionable.SiteWideMaintenance
                ) { "Intent ViewStatus called with incorrect status ${modelState.status}" }

                openUrl(modelState.status.statusUrl)
            }

            AppMaintenanceIntents.SkipUpdate -> {
                require(
                    modelState.status is AppMaintenanceStatus.Actionable.OptionalUpdate
                ) { "Intent SkipUpdate called with incorrect status ${modelState.status}" }

                resumeAppFlow()
            }

            AppMaintenanceIntents.UpdateApp -> {
                require(
                    modelState.status is AppMaintenanceStatus.Actionable.OptionalUpdate ||
                        modelState.status is AppMaintenanceStatus.Actionable.MandatoryUpdate
                ) { "Intent UpdateApp called with incorrect status ${modelState.status}" }

                when (modelState.status) {
                    is AppMaintenanceStatus.Actionable.MandatoryUpdate -> modelState.status.updateLocation
                    is AppMaintenanceStatus.Actionable.OptionalUpdate -> modelState.status.updateLocation
                    else -> UpdateLocation.InAppUpdate
                }.run {
                    when (this) {
                        UpdateLocation.InAppUpdate -> launchAppUpdate()
                        is UpdateLocation.ExternalUrl -> openUrl(url)
                    }
                }
            }
        }.exhaustive
    }

    /**
     * If the download was already triggered in a previous session, and is still in progress
     * -> show the play store ui
     */
    private fun checkDownloadStatus() {
        viewModelScope.launch {
            if (isDownloadInProgressUseCase()) navigate(AppMaintenanceNavigationEvent.LaunchAppUpdate)
        }
    }

    /**
     * * If the status is [AppMaintenanceStatus.NonActionable.Unknown] for whatever reason
     * -> resume the flow
     *
     * * If the status is [AppMaintenanceStatus.NonActionable.AllClear]
     * -> resume the flow - this should generally never happen as this screen should not be triggered in that case
     *
     * * If the status is an [AppMaintenanceStatus.Actionable]
     * -> update the screen
     */
    private fun getAppMaintenanceStatus() {
        viewModelScope.launch {
            getAppMaintenanceConfigUseCase().let { status ->
                when (status) {
                    AppMaintenanceStatus.NonActionable.Unknown,
                    AppMaintenanceStatus.NonActionable.AllClear -> {
                        resumeAppFlow()
                    }

                    is AppMaintenanceStatus.Actionable -> {
                        updateState { it.copy(status = status) }
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        navigate(AppMaintenanceNavigationEvent.OpenUrl(url))
    }

    private fun launchAppUpdate() {
        navigate(AppMaintenanceNavigationEvent.LaunchAppUpdate)
    }

    /**
     * Navigate (resume) wherever the app was suspended to show the maintenance screen
     */
    private fun resumeAppFlow() = navigate(AppMaintenanceNavigationEvent.ResumeAppFlow)
}
