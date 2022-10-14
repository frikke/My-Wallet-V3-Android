package piuk.blockchain.android.ui.settings.sheets.sms

import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.nabu.NabuUserSync
import com.blockchain.utils.thenSingle
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class SMSVerificationInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync,
    private val payloadDataManager: PayloadDataManager
) {

    private val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())

    fun resendCodeSMS(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix)
            .firstOrError()
            .flatMap {
                syncPhoneNumberWithNabu().thenSingle {
                    updateNotification(Settings.NOTIFICATION_TYPE_SMS, false)
                }
            }

    fun verifyPhoneNumber(code: String): Completable {
        return settingsDataManager.verifySms(code)
            .flatMapCompletable { syncPhoneNumberWithNabu() }
    }

    private fun syncPhoneNumberWithNabu(): Completable {
        return nabuUserSync.syncUser()
    }

    private fun updateNotification(type: Int, enable: Boolean): Single<Settings> {
        return cachedSettings.flatMap {
            if (enable && it.isNotificationTypeEnabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            } else if (!enable && it.isNotificationTypeDisabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            }
            val notificationsUpdate =
                if (enable) settingsDataManager.enableNotification(type, it.notificationsType)
                else settingsDataManager.disableNotification(type, it.notificationsType)
            notificationsUpdate.flatMapCompletable {
                if (enable) {
                    payloadDataManager.syncPayloadAndPublicKeys()
                } else {
                    Completable.complete()
                }
            }.thenSingle {
                cachedSettings
            }
        }
    }

    private fun Settings.isNotificationTypeEnabled(type: Int): Boolean {
        return isNotificationsOn && (
            notificationsType.contains(type) ||
                notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL)
            )
    }

    private fun Settings.isNotificationTypeDisabled(type: Int): Boolean {
        return notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_NONE) ||
            (
                !notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL) &&
                    !notificationsType.contains(type)
                )
    }
}
