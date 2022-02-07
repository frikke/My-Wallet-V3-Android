package piuk.blockchain.android.ui.settings.v2.notifications

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class NotificationsModel(
    initialState: NotificationsState,
    mainScheduler: Scheduler,
    private val interactor: NotificationsInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<NotificationsState, NotificationsIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: NotificationsState,
        intent: NotificationsIntent
    ): Disposable? =
        when (intent) {
            is NotificationsIntent.LoadNotificationInfo -> interactor.getNotificationsEnabled().subscribeBy(
                onSuccess = { (emailNotificationsEnabled, pushNotificationsEnabled) ->
                    process(
                        NotificationsIntent.UpdateNotificationValues(
                            emailNotificationsEnabled, pushNotificationsEnabled
                        )
                    )
                },
                onError = {
                    Timber.e("Error getting notification settings $it")
                    process(
                        NotificationsIntent.UpdateErrorState(
                            NotificationsError.NOTIFICATION_INFO_LOAD_FAIL
                        )
                    )
                }
            )
            is NotificationsIntent.ToggleEmailNotifications -> {
                interactor.toggleEmailNotifications(previousState.emailNotificationsEnabled)
                    .subscribeBy(
                        onComplete = {
                            process(
                                NotificationsIntent.UpdateNotificationValues(
                                    emailNotificationsEnabled = !previousState.emailNotificationsEnabled,
                                    pushNotificationsEnabled = previousState.pushNotificationsEnabled
                                )
                            )
                        },
                        onError = {
                            Timber.e("Error updating email notifications $it")
                            process(
                                NotificationsIntent.UpdateErrorState(
                                    if (it is EmailNotVerifiedException) {
                                        NotificationsError.EMAIL_NOT_VERIFIED
                                    } else {
                                        NotificationsError.EMAIL_NOTIFICATION_UPDATE_FAIL
                                    }
                                )
                            )
                        }
                    )
            }
            is NotificationsIntent.TogglePushNotifications -> {
                if (interactor.arePushNotificationsEnabled()) {
                    interactor.disablePushNotifications()
                } else {
                    interactor.enablePushNotifications()
                }.subscribeBy(
                    onComplete = {
                        process(
                            NotificationsIntent.UpdateNotificationValues(
                                emailNotificationsEnabled = previousState.emailNotificationsEnabled,
                                pushNotificationsEnabled = !previousState.pushNotificationsEnabled
                            )
                        )
                    },
                    onError = {
                        Timber.e("Error updating push notifications $it")
                        process(
                            NotificationsIntent.UpdateErrorState(
                                NotificationsError.PUSH_NOTIFICATION_UPDATE_FAIL
                            )
                        )
                    }
                )
            }
            is NotificationsIntent.UpdateNotificationValues,
            is NotificationsIntent.UpdateErrorState,
            is NotificationsIntent.ResetErrorState -> null
        }.exhaustive
}
