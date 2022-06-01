package piuk.blockchain.com

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCategory

class WalletModeRepository(private val sharedPreferences: SharedPreferences) : WalletModeService {
    override fun enabledWalletTypes(): Set<AssetCategory> {
        val walletModeStringSet = sharedPreferences.getStringSet(
            WALLET_MODE,
            AssetCategory.values().map { it.name }.toSet()
        )
        return AssetCategory.values().filter { walletModeStringSet?.contains(it.name) ?: false }.toSet()
    }

    override fun updateEnabledWalletTypes(types: Set<AssetCategory>) {
        sharedPreferences.edit {
            putStringSet(WALLET_MODE, types.map { it.name }.toSet())
        }
    }
}

private const val WALLET_MODE = "WALLET_MODE"
