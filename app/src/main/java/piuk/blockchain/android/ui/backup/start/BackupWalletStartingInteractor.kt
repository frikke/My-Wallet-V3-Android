package piuk.blockchain.android.ui.backup.start

import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs

class BackupWalletStartingInteractor(
    private val prefs: SessionPrefs,
    private val settingsDataManager: SettingsDataManager
) {
    fun triggerSeedPhraseAlert() =
        settingsDataManager.triggerEmailAlert(
            guid = prefs.walletGuid,
            sharedKey = prefs.sharedKey
        )
}
