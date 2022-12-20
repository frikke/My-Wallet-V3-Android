package piuk.blockchain.android.ui.settings.appprefs

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class LocalSettingsIntent : Intent<LocalSettingsModelState> {
    object LoadLocalSettings : LocalSettingsIntent()
    class ToggleChartVibration(val isVibrationEnabled: Boolean) : LocalSettingsIntent()
    class ToggleSmallBalances(val areSmallBalancesEnabled: Boolean) : LocalSettingsIntent()
}
