package java.piuk.blockchain.com

import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCategory

class WalletModeRepository : WalletModeService {
    override fun supportedWalletModes(): Set<AssetCategory> {
        return AssetCategory.values().toSet()
    }

    override fun updateSupportedWalletModes(types: Set<AssetCategory>) {
        throw UnsupportedOperationException("Not supported on release ")
    }
}
