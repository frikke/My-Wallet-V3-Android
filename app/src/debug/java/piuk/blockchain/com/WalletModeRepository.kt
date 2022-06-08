package piuk.blockchain.com

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService

class WalletModeRepository(private val sharedPreferences: SharedPreferences) : WalletModeService {
    override fun enabledWalletMode(): WalletMode {
        val walletModeString = sharedPreferences.getString(
            WALLET_MODE,
            ""
        )
        return WalletMode.values().firstOrNull { walletModeString == it.name } ?: WalletMode.UNIVERSAL
    }

    override fun updateEnabledWalletMode(type: WalletMode) {
        sharedPreferences.edit {
            putString(WALLET_MODE, type.name)
        }
    }
}

private const val WALLET_MODE = "WALLET_MODE"
