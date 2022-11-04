package piuk.blockchain.android.walletmode

import com.blockchain.coincore.total
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.combineDataResources
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Money
import info.blockchain.balance.total
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class WalletModeBalanceRepository(
    private var walletModeService: WalletModeService,
    private val balanceStore: WalletModeBalanceCache,
    private val currencyPrefs: CurrencyPrefs,
) : WalletModeBalanceService {

    override fun balanceFor(walletMode: WalletMode): Flow<DataResource<Money>> {
        return getBalance(walletMode, FreshnessStrategy.Cached(forceRefresh = false))
    }

    override fun totalBalance(): Flow<DataResource<Money>> {
        val balances = walletModeService.availableModes().map { walletMode ->
            balanceFor(walletMode)
        }

        return combine(balances) { balancesArray ->
            combineDataResources(balancesArray.toList()) { balancesList ->
                balancesList.total()
            }
        }
    }

    override fun getBalanceWithFailureState(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Pair<Money, Boolean>>> {
        return balanceStore.stream(freshnessStrategy.withKey(walletMode)).mapData {
            val anyFailed = it.any { (_, balance) -> balance == null }
            val totalBalance = it.mapNotNull { (_, balance) -> balance }.total(currencyPrefs.selectedFiatCurrency).total
            Pair(totalBalance, anyFailed)
        }
    }

    private fun getBalance(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Money>> {
        return getBalanceWithFailureState(walletMode, freshnessStrategy)
            .mapData { (balance, _) -> balance }
    }
}
