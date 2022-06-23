package piuk.blockchain.com

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import io.reactivex.rxjava3.core.Observable

class WalletModeRepository : WalletModeService {

    override fun enabledWalletMode(): WalletMode {
        return WalletMode.UNIVERSAL
    }

    override val walletMode: Observable<WalletMode>
        get() = Observable.just(WalletMode.UNIVERSAL)

    override fun updateEnabledWalletMode(type: WalletMode) {
        throw UnsupportedOperationException("Not supported on release ")
    }
}
