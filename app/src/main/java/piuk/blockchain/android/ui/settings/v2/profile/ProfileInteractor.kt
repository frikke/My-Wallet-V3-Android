package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class ProfileInteractor internal constructor(
    private val emailUpdater: EmailSyncUpdater,
    private val prefs: PersistentPrefs,
    private val settingsDataManager: SettingsDataManager
) {

    // TODO make call to the fields that have changed
    fun saveProfile(email: String, phone: String): Single<Pair<Email, Settings>> =
        Singles.zip(
            emailUpdater.updateEmailAndSync(email),
            settingsDataManager.updateSms(phone).singleOrError()
        )

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = prefs.walletGuid,
            sharedKey = prefs.sharedKey
        )
}
