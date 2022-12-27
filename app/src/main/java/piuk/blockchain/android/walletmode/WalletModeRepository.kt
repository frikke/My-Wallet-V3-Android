package piuk.blockchain.android.walletmode

import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

open class WalletModeRepository(
    private val walletModeStore: WalletModeStore,
    private val defaultWalletModeStrategy: DefaultWalletModeStrategy
) : WalletModeService {

    private val _walletMode: MutableSharedFlow<WalletMode> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun reset() {
        _walletMode.resetReplayCache()
    }

    override val walletMode: Flow<WalletMode>
        get() = _walletMode.distinctUntilChanged().onStart {
            walletModeStore.walletMode?.let {
                emit(it)
            } ?: emit(
                defaultWalletModeStrategy.defaultWalletMode().also {
                    walletModeStore.updateWalletMode(it)
                }
            )
        }

    override suspend fun updateEnabledWalletMode(type: WalletMode) {
        walletModeStore.updateWalletMode(type).also {
            _walletMode.emit(type)
        }
    }

    override fun availableModes(): List<WalletMode> = WalletMode.values().toList()
}
