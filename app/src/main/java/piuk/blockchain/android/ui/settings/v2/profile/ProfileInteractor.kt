package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.preferences.AuthPrefs
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class ProfileInteractor internal constructor(
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager
) {
    fun fetchProfileSettings(): Single<WalletSettingsService.UserInfoSettings> =
        settingsDataManager.fetchWalletSettings(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )

    val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())
}
