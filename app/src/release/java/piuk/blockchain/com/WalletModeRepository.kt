package piuk.blockchain.com

import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCategory

class WalletModeRepository : WalletModeService {
    override fun enabledWalletTypes(): Set<AssetCategory> {
        return AssetCategory.values().toSet()
    }

    override fun updateEnabledWalletTypes(types: Set<AssetCategory>) {
        throw UnsupportedOperationException("Not supported on release ")
    }
}
