package piuk.blockchain.android.ui.settings.v2.profile.email

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class EmailInteractor internal constructor(
    private val emailUpdater: EmailSyncUpdater,
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager,
    private val getUserStore: GetUserStore
) {

    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )

    val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())

    val invalidateCache
        get() = settingsDataManager.clearSettingsCache()

    /*
       BE eventually will sync and update notifications when user updates email and phone number
       keep an eye: https://blockchain.atlassian.net/browse/WS-171
    */
    fun saveEmail(email: String): Single<Email> =
        emailUpdater.updateEmailAndSync(email)
            .doOnSuccess {
                userDataSource.invalidate()
            }

    fun resendEmail(email: String): Single<Email> = emailUpdater.resendEmail(email)
}
