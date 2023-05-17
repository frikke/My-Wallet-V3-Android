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

    override fun LocalSettingsModelState.reduce() = when (localSettings) {
        is DataResource.Data -> LocalSettingsViewState.Data(
            isChartVibrationEnabled = localSettings.data.isChartVibrationEnabled,
            areSmallBalancesEnabled = localSettings.data.areSmallBalancesEnabled
        )

        is DataResource.Error -> LocalSettingsViewState.Error(localSettings.error.message.orEmpty())

        DataResource.Loading -> LocalSettingsViewState.Loading
    }

    override suspend fun handleIntent(modelState: LocalSettingsModelState, intent: LocalSettingsIntent) =
        when (intent) {
            LocalSettingsIntent.LoadLocalSettings -> {
                updateState {
                    copy(
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
                updateState {
                    if (localSettings is DataResource.Data) {
                        copy(
                            localSettings = localSettings.map { settings ->
                                settings.copy(
                                    isChartVibrationEnabled = intent.isVibrationEnabled
                                )
                            }
                        )
                    } else {
                        this
                    }
                }
            }

            is LocalSettingsIntent.ToggleSmallBalances -> {
                localSettingsPrefs.hideSmallBalancesEnabled = intent.areSmallBalancesEnabled

                updateState {
                    if (localSettings is DataResource.Data) {
                        copy(
                            localSettings = localSettings.map { settings ->
                                settings.copy(
                                    areSmallBalancesEnabled = intent.areSmallBalancesEnabled
                                )
                            }
                        )
                    } else {
                        this
                    }
                }
            }
        }
}
