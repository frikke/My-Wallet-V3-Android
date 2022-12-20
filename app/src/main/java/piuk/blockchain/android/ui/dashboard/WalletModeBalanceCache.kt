package piuk.blockchain.android.ui.dashboard

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import com.blockchain.walletmode.WalletMode
import io.reactivex.rxjava3.core.Single

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
)
