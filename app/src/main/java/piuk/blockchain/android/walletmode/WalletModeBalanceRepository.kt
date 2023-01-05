package piuk.blockchain.android.walletmode

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.total
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import info.blockchain.balance.Money
import info.blockchain.balance.total
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class WalletModeBalanceRepository(
    private val balanceStore: WalletModeBalanceCache,
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
) : WalletModeBalanceService {

    override fun balanceFor(walletMode: WalletMode): Flow<DataResource<Money>> {
        return getBalance(walletMode, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
    }

    override fun totalBalance(): Flow<DataResource<Money>> {
        val balances = WalletMode.values().map {
            coincore.activeWalletsInMode(it).flatMapLatest { it.balance }.map { it.total }
        }
        return combine(balances) {
            it.toList().total()
        }.map {
            DataResource.Data(it)
        }.catch {
            flowOf(DataResource.Error(it as Exception))
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
