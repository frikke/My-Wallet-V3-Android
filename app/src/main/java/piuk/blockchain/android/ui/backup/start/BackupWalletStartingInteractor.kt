package piuk.blockchain.android.ui.backup.start

import com.blockchain.preferences.AuthPrefs
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs

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
