package piuk.blockchain.android.ui.backup.start

import com.blockchain.preferences.AuthPrefs
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class BackupWalletStartingInteractor(
    private val authPrefs: AuthPrefs,
    private val settingsDataManager: SettingsDataManager
) {
    fun triggerSeedPhraseAlert() =
        settingsDataManager.triggerEmailAlert(
            guid = authPrefs.walletGuid,
            sharedKey = authPrefs.sharedKey
        )
}
