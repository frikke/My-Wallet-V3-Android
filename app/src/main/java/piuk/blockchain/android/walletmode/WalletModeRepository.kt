package piuk.blockchain.android.walletmode

import androidx.lifecycle.lifecycleScope
import com.blockchain.analytics.AnalyticsSettings
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class WalletModeRepository(
    private val walletModeStore: WalletModeStore,
    private val defaultWalletModeStrategy: DefaultWalletModeStrategy,
    private val analyticsSettings: AnalyticsSettings,
    private val coroutineScope: CoroutineScope
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
        coroutineScope.launch {
            _walletMode.firstOrNull()?.let { currentWalletMode ->
                if (currentWalletMode != type) {
                    analyticsSettings.flush(overrideWalletMode = currentWalletMode)
                        .onErrorComplete()
                        .await()
                }
            }
        }

        walletModeStore.updateWalletMode(type)
            .also {
                _walletMode.emit(type)
            }
    }

    override suspend fun availableModes(): List<WalletMode> = WalletMode.values().toList().run {
        if (defaultWalletModeStrategy.custodialEnabled().not()) {
            minus(WalletMode.CUSTODIAL)
        } else {
            this
        }
    }
}
