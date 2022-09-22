package piuk.blockchain.android.walletmode

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SuperAppWalletModeRepository(private val walletModeStore: WalletModeStore) : WalletModeService {

    private var _walletMode: MutableStateFlow<WalletMode> = MutableStateFlow(enabledWalletMode())

    override fun enabledWalletMode(): WalletMode {
        val persistedMode = walletModeStore.walletMode
        return if (persistedMode == WalletMode.UNIVERSAL) superAppDefMode else persistedMode
    }

    override val walletMode: Flow<WalletMode>
        get() = _walletMode

    override fun reset() {
        _walletMode = MutableStateFlow(enabledWalletMode())
    }

    override fun updateEnabledWalletMode(type: WalletMode) {
        require(type in availableModes())
        walletModeStore.updateWalletMode(type)
        _walletMode.value = type
    }

    override fun availableModes(): List<WalletMode> = listOf(
        WalletMode.NON_CUSTODIAL_ONLY,
        WalletMode.CUSTODIAL_ONLY
    )

    companion object {
        private val superAppDefMode: WalletMode = WalletMode.CUSTODIAL_ONLY
    }
}
