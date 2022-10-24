package piuk.blockchain.android.ui.settings.appprefs

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.preferences.LocalSettingsPrefs

class LocalSettingsViewModel(
    private val localSettingsPrefs: LocalSettingsPrefs
) : MviViewModel<
    LocalSettingsIntent,
    LocalSettingsViewState,
    LocalSettingsModelState,
    LocalSettingsNavigation,
    ModelConfigArgs.NoArgs
    >(LocalSettingsModelState(DataResource.Loading)) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        // do nothing
    }

    override fun reduce(state: LocalSettingsModelState): LocalSettingsViewState =
        when (val modelState = state.localSettings) {
            is DataResource.Data ->
                LocalSettingsViewState.Data(
                    isChartVibrationEnabled = modelState.data.isChartVibrationEnabled,
                    areSmallBalancesEnabled = modelState.data.areSmallBalancesEnabled
                )
            is DataResource.Error -> LocalSettingsViewState.Error(modelState.error.message.orEmpty())
            DataResource.Loading -> LocalSettingsViewState.Loading
        }

    override suspend fun handleIntent(modelState: LocalSettingsModelState, intent: LocalSettingsIntent) =
        when (intent) {
            LocalSettingsIntent.LoadLocalSettings -> {
                updateState {
                    it.copy(
                        localSettings = DataResource.Data(
                            LocalSettings(
                                isChartVibrationEnabled = localSettingsPrefs.isChartVibrationEnabled,
                                areSmallBalancesEnabled = localSettingsPrefs.hideSmallBalancesEnabled
                            )
                        )
                    )
                }
            }
            is LocalSettingsIntent.ToggleChartVibration -> {
                localSettingsPrefs.isChartVibrationEnabled = intent.isVibrationEnabled
                updateState { state ->
                    if (state.localSettings is DataResource.Data) {
                        state.copy(
                            localSettings = state.localSettings.map { settings ->
                                settings.copy(
                                    isChartVibrationEnabled = intent.isVibrationEnabled
                                )
                            }
                        )
                    } else {
                        state
                    }
                }
            }
            is LocalSettingsIntent.ToggleSmallBalances -> {
                localSettingsPrefs.hideSmallBalancesEnabled = intent.areSmallBalancesEnabled

                updateState { state ->
                    if (state.localSettings is DataResource.Data) {
                        state.copy(
                            localSettings = state.localSettings.map { settings ->
                                settings.copy(
                                    areSmallBalancesEnabled = intent.areSmallBalancesEnabled
                                )
                            }
                        )
                    } else {
                        state
                    }
                }
            }
        }
}
