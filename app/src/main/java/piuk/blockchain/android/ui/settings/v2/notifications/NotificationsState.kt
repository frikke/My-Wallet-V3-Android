package piuk.blockchain.android.ui.settings.v2.notifications

import com.blockchain.commonarch.presentation.mvi.MviState

data class NotificationsState(
    val emailNotificationsEnabled: Boolean = false,
    val pushNotificationsEnabled: Boolean = false,
    val errorState: NotificationsError = NotificationsError.NONE
) : MviState

enum class NotificationsError {
    NONE,
    EMAIL_NOTIFICATION_UPDATE_FAIL,
    EMAIL_NOT_VERIFIED,
    PUSH_NOTIFICATION_UPDATE_FAIL,
    NOTIFICATION_INFO_LOAD_FAIL
}

internal class EmailNotVerifiedException : Throwable()
