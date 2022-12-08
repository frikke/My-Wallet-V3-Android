package piuk.blockchain.android.walletmode

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class SuperAppWalletModeRepository(private val walletModeStore: WalletModeStore) : WalletModeService {

    private val _walletMode: MutableSharedFlow<WalletMode> = MutableSharedFlow(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun enabledWalletMode(): WalletMode {
        val persistedMode = walletModeStore.walletMode
        return if (persistedMode == WalletMode.UNIVERSAL) superAppDefMode else persistedMode
    }

    override fun start() {
        _walletMode.tryEmit(enabledWalletMode())
    }

    override val walletMode: Flow<WalletMode>
        get() = _walletMode.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reset() {
        _walletMode.resetReplayCache()
    }

    override fun updateEnabledWalletMode(type: WalletMode) {
        require(type in availableModes())
        walletModeStore.updateWalletMode(type)
        _walletMode.tryEmit(type)
    }

    override fun availableModes(): List<WalletMode> = listOf(
        WalletMode.NON_CUSTODIAL_ONLY,
        WalletMode.CUSTODIAL_ONLY
    )

    companion object {
        private val superAppDefMode: WalletMode = WalletMode.CUSTODIAL_ONLY
    }
}
