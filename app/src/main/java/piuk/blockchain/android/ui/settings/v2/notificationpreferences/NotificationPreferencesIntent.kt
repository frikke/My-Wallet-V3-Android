package piuk.blockchain.android.ui.settings.v2.notificationpreferences

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class NotificationPreferencesIntent : Intent<NotificationPreferencesModelState> {
    object Fetch : NotificationPreferencesIntent()
    object Retry : NotificationPreferencesIntent()
    data class NavigateToDetails(val preferenceId: Int) : NotificationPreferencesIntent()
    object NavigateBack : NotificationPreferencesIntent()
}
