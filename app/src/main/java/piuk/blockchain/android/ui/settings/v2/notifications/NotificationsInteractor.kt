package piuk.blockchain.android.ui.settings.v2.notifications

import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.preferences.NotificationPrefs
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class NotificationsInteractor internal constructor(
    private val notificationPrefs: NotificationPrefs,
    private val notificationTokenManager: NotificationTokenManager,
    private val settingsDataManager: SettingsDataManager,
    private val payloadDataManager: PayloadDataManager
) {

    fun getNotificationsEnabled(): Single<Pair<Boolean, Boolean>> =
        settingsDataManager.getSettings().firstOrError().zipWith(arePushNotificationsEnabled())
            .map { (settings, pushNotificationsEnabled) ->
                val emailNotificationsEnabled = settings.notificationsType.any {
                    it == Settings.NOTIFICATION_TYPE_EMAIL ||
                        it == Settings.NOTIFICATION_TYPE_ALL
                }

                Pair(emailNotificationsEnabled, pushNotificationsEnabled)
            }

    fun enablePushNotifications(): Completable = notificationTokenManager.enableNotifications()

    fun disablePushNotifications(): Completable = notificationTokenManager.disableNotifications()

    fun arePushNotificationsEnabled(): Single<Boolean> = rxSingle(Schedulers.io().asCoroutineDispatcher()) {
        notificationPrefs.arePushNotificationsEnabled()
    }

    fun toggleEmailNotifications(areEmailNotificationsEnabled: Boolean): Completable =
        settingsDataManager.getSettings().firstOrError().flatMapCompletable {
            if (!it.isEmailVerified) {
                Completable.error(EmailNotVerifiedException())
            } else {
                val notificationsUpdate = if (areEmailNotificationsEnabled) {
                    settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, it.notificationsType)
                } else {
                    settingsDataManager.enableNotification(Settings.NOTIFICATION_TYPE_EMAIL, it.notificationsType)
                }
                notificationsUpdate.flatMapCompletable {
                    if (areEmailNotificationsEnabled) {
                        payloadDataManager.syncPayloadAndPublicKeys()
                    } else {
                        Completable.complete()
                    }
                }
            }
        }
}
