package piuk.blockchain.com

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService

class WalletModeRepository : WalletModeService {

    override fun enabledWalletMode(): WalletMode {
        return WalletMode.UNIVERSAL
    }

    override fun updateEnabledWalletMode(type: WalletMode) {
        throw UnsupportedOperationException("Not supported on release ")
    }
}
