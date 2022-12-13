package piuk.blockchain.android.walletmode

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeStore

class SuperAppWalletModeRepository(
    walletModeStore: WalletModeStore,
    defaultWalletModeStrategy: DefaultWalletModeStrategy
) : WalletModeRepository(
    walletModeStore, defaultWalletModeStrategy
) {
    override fun availableModes(): List<WalletMode> = listOf(
        WalletMode.NON_CUSTODIAL_ONLY,
        WalletMode.CUSTODIAL_ONLY
    )
}
