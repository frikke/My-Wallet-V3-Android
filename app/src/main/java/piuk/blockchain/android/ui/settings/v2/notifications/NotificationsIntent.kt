package piuk.blockchain.android.ui.settings.v2.notifications

import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class NotificationsIntent : MviIntent<NotificationsState> {

    object LoadNotificationInfo : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState
    }

    class UpdateNotificationValues(
        private val emailNotificationsEnabled: Boolean,
        private val pushNotificationsEnabled: Boolean
    ) : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState.copy(
            emailNotificationsEnabled = emailNotificationsEnabled,
            pushNotificationsEnabled = pushNotificationsEnabled
        )
    }

    object ToggleEmailNotifications : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState
    }

    object TogglePushNotifications : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState
    }

    object ResetErrorState : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState.copy(
            errorState = NotificationsError.NONE
        )
    }

    class UpdateErrorState(private val error: NotificationsError) : NotificationsIntent() {
        override fun reduce(oldState: NotificationsState): NotificationsState = oldState.copy(
            errorState = error
        )
    }
}
