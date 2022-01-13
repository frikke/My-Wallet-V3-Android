package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class ProfileInteractor internal constructor(
    private val emailUpdater: EmailSyncUpdater,
    private val prefs: PersistentPrefs,
    private val settingsDataManager: SettingsDataManager,
    private val nabuUserSync: NabuUserSync
) {

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = prefs.walletGuid,
            sharedKey = prefs.sharedKey
        )

    fun saveEmail(email: String): Single<Settings> =
        settingsDataManager.updateEmail(email).firstOrError()

    fun resendEmail(email: String): Single<Email> = emailUpdater.resendEmail(email)

    fun savePhoneNumber(mobileWithPrefix: String): Single<Settings> =
        settingsDataManager.updateSms(mobileWithPrefix).firstOrError()

    fun resendCodeSMS(mobileWithPrefix: String): Single<Settings> {
        return settingsDataManager.updateSms(mobileWithPrefix).singleOrError()
    }

    fun verifyPhoneNumber(code: String): Completable {
        return settingsDataManager.verifySms(code)
            .flatMapCompletable { syncPhoneNumberWithNabu() }
    }

    private fun syncPhoneNumberWithNabu(): Completable {
        return nabuUserSync.syncUser()
    }
}
