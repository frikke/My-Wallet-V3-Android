package piuk.blockchain.android.walletmode

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.blockchain.walletmode.WalletModeStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class WalletModeRepository(
    private val walletModeStore: WalletModeStore,
    private val featureFlag: IntegratedFeatureFlag,
) : WalletModeService {
    private var walletModesEnabled = false
    private var _walletMode: MutableStateFlow<WalletMode> = MutableStateFlow(WalletMode.UNIVERSAL)

    init {
        GlobalScope.launch {
            while (true) {
                walletModesEnabled = featureFlag.coEnabled()
                /**
                 * If its universal then update to the enabled.
                 */
                _walletMode.compareAndSet(WalletMode.UNIVERSAL, enabledWalletMode())
                delay(ONE_HOUR_MILLIS)
            }
        }
    }

    override fun enabledWalletMode(): WalletMode {
        if (!walletModesEnabled)
            return WalletMode.UNIVERSAL

        return walletModeStore.walletMode
    }

    override fun reset() {
        _walletMode = MutableStateFlow(enabledWalletMode())
    }

    override val walletMode: Flow<WalletMode>
        get() = _walletMode

    override fun updateEnabledWalletMode(type: WalletMode) {
        walletModeStore.updateWalletMode(type).also {
            _walletMode.value = type
        }
    }

    override fun availableModes(): List<WalletMode> = WalletMode.values().toList()
}

private const val ONE_HOUR_MILLIS = 3600000L
