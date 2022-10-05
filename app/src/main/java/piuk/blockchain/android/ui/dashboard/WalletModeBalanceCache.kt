package piuk.blockchain.android.ui.dashboard

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_inmemory.InMemoryCacheStoreBuilder
import com.blockchain.walletmode.WalletMode

class WalletModeBalanceCache(private val coincore: Coincore) : KeyedStore<
    WalletMode,
    AccountBalance
    > by InMemoryCacheStoreBuilder().buildKeyed(
    storeId = "WalletModeBalanceCache",
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { walletMode: WalletMode ->
            coincore.activeWalletsInModeRx(walletMode).firstOrError().flatMap { it.balanceRx.firstOrError() }
        }
    ),
    mediator = FreshnessMediator(Freshness.ofMinutes(30))
)
