package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.NabuUserSync
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
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

    fun saveProfile(email: String, mobileWithPrefix: String): Single<Pair<Email, Settings>> =
        Singles.zip(
            emailUpdater.updateEmailAndSync(email),
            settingsDataManager.updateSms(mobileWithPrefix).singleOrError()
        )

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = prefs.walletGuid,
            sharedKey = prefs.sharedKey
        )

    fun saveAndSendEmail(email: String): Single<Email> {
        return emailUpdater.updateEmailAndSync(email)
    }

    fun saveAndSendSMS(mobileWithPrefix: String): Single<Settings> {
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
