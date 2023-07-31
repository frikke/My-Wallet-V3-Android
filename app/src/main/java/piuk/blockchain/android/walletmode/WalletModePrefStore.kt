package piuk.blockchain.android.walletmode

import com.blockchain.preferences.WalletModePrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeStore

class WalletModePrefStore(
    private val walletModePrefs: WalletModePrefs
) : WalletModeStore {
    override fun updateWalletMode(walletMode: WalletMode) {
        walletModePrefs.currentWalletMode = walletMode.name
    }

    override val walletMode: WalletMode?
        get() {
            val walletModeString = walletModePrefs.currentWalletMode
            return try {
                WalletMode.valueOf(walletModeString)
            } catch (e: Exception) {
                null
            }
        }
}
