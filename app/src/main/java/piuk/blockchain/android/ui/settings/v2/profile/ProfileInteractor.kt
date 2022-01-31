package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class ProfileInteractor internal constructor(
    private val emailUpdater: EmailSyncUpdater,
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync,
    private val payloadDataManager: PayloadDataManager
) {

    private val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )

    /*
       BE eventually will sync and update notifications when user updates email and phone number
       keep an eye: https://blockchain.atlassian.net/browse/WS-171
    */
    fun saveEmail(email: String): Single<Settings> =
        emailUpdater.updateEmailAndSync(email).flatMap {
            updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false)
        }

    fun resendEmail(email: String): Single<Email> = emailUpdater.resendEmail(email)

    fun savePhoneNumber(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix, forceJson = true)
            .flatMap {
                syncPhoneNumberWithNabu().thenSingle {
                    updateNotification(Settings.NOTIFICATION_TYPE_SMS, false)
                }
            }
    /*
        Eventually "resend-sms" without having to save the phone number in order to get a SMS,
        keep an eye: https://blockchain.atlassian.net/browse/WS-170
    */
    fun resendCodeSMS(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix, forceJson = true)
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
                    payloadDataManager.syncPayloadWithServer()
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
