package piuk.blockchain.android.walletmode

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeStore

class WalletModePrefStore(private val sharedPreferences: SharedPreferences) : WalletModeStore {
    override fun updateWalletMode(walletMode: WalletMode) {
        sharedPreferences.edit {
            putString(WALLET_MODE, walletMode.name)
        }
    }

    override val walletMode: WalletMode
        get() {
            val walletModeString = sharedPreferences.getString(
                WALLET_MODE,
                ""
            )
            return WalletMode.values().firstOrNull { walletModeString == it.name } ?: WalletMode.CUSTODIAL_ONLY
        }
}

private const val WALLET_MODE = "WALLET_MODE"
