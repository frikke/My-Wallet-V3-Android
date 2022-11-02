package piuk.blockchain.android.ui.dashboard

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.total
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store.mapData
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

class WalletModeBalanceCache(private val coincore: Coincore) : KeyedStore<
    WalletMode,
    Map<SingleAccount, AccountBalance?>
    > by InMemoryCacheStoreBuilder().buildKeyed(
    storeId = "WalletModeBalanceCache",
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { walletMode: WalletMode ->
            coincore.activeWalletsInModeRx(walletMode).firstOrError().map { it.accounts }.flatMap { accounts ->
                if (accounts.isEmpty()) {
                    Single.just(emptyMap())
                } else
                    Single.just(accounts).flattenAsObservable { it }.flatMapSingle { account ->
                        account.balanceRx.firstOrError().map { balance ->
                            account to balance
                        }.onErrorReturn {
                            account to null
                        }
                    }.toList().map {
                        it.toMap()
                    }
            }
        }
    ),
    mediator = FreshnessMediator(Freshness.ofMinutes(30))
) {
    fun getBalanceWithFailureState(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Pair<AccountBalance, Boolean>>> {
        return stream(freshnessStrategy.withKey(walletMode)).mapData {
            val anyFailed = it.any { (_, balance) -> balance == null }
            val totalBalance = it.mapNotNull { (_, balance) -> balance }.total()

            Pair(totalBalance, anyFailed)
        }
    }

    fun getBalance(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<AccountBalance>> {
        return getBalanceWithFailureState(walletMode, freshnessStrategy)
            .mapData { (balance, _) -> balance }
    }
}
