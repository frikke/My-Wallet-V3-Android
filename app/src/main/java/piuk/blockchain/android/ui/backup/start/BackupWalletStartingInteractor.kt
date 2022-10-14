package piuk.blockchain.android.ui.backup.start

import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.preferences.AuthPrefs

class BackupWalletStartingInteractor(
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager
) {
    fun triggerSeedPhraseAlert() =
        settingsDataManager.triggerEmailAlertLegacy(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )
}
