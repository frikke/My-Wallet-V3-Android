package piuk.blockchain.android.walletmode

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class WalletModeRepository(
    private val walletModeStore: WalletModeStore,
) : WalletModeService {

    private val _walletMode: MutableSharedFlow<WalletMode> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun enabledWalletMode(): WalletMode {
        return walletModeStore.walletMode
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reset() {
        _walletMode.resetReplayCache()
    }

    override val walletMode: Flow<WalletMode>
        get() = _walletMode.distinctUntilChanged()

    override fun start() {
        _walletMode.tryEmit(enabledWalletMode())
    }

    override fun updateEnabledWalletMode(type: WalletMode) {
        walletModeStore.updateWalletMode(type).also {
            _walletMode.tryEmit(type)
        }
    }

    override fun availableModes(): List<WalletMode> = WalletMode.values().toList()
}
