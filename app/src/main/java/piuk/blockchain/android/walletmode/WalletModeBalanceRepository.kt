package piuk.blockchain.android.walletmode

import com.blockchain.coincore.Coincore
import com.blockchain.coincore.total
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeBalanceService
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asFlow
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache

class WalletModeBalanceRepository(
    private val balanceStore: WalletModeBalanceCache,
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs
) : WalletModeBalanceService {

    override fun balanceFor(walletMode: WalletMode, freshnessStrategy: FreshnessStrategy): Flow<DataResource<Money>> {
        return getBalance(walletMode, freshnessStrategy)
    }

    override fun totalBalance(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Money>> {
        return coincore.allActiveWallets(freshnessStrategy).flatMap {
            it.balanceRx(freshnessStrategy)
        }.map {
            DataResource.Data(it.total) as DataResource<Money>
        }.onErrorResumeNext {
            Observable.just(DataResource.Error(it as Exception))
        }.asFlow()
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
