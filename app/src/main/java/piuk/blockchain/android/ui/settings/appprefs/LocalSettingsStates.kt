package piuk.blockchain.android.ui.settings.appprefs

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

sealed class LocalSettingsViewState : ViewState {
    object Loading : LocalSettingsViewState()
    class Data(
        val isChartVibrationEnabled: Boolean,
        val areSmallBalancesEnabled: Boolean
    ) : LocalSettingsViewState()

    class Error(message: String) : LocalSettingsViewState()
}

data class LocalSettingsModelState(
    val localSettings: DataResource<LocalSettings>
) : ModelState

data class LocalSettings(
    val isChartVibrationEnabled: Boolean,
    val areSmallBalancesEnabled: Boolean
)

sealed class LocalSettingsNavigation : NavigationEvent
