package piuk.blockchain.android.domain.repositories

import com.blockchain.caching.ExpiringRepository
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.TransactionType
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import timber.log.Timber

class AssetActivityRepository(
    private val coincore: Coincore
) : ExpiringRepository<ActivitySummaryList>() {

    private val transactionCache = mutableListOf<ActivitySummaryItem>()

    fun fetch(
        account: BlockchainAccount,
        isRefreshRequested: Boolean
    ): Observable<ActivitySummaryList> {
        val cacheMaybe = if (isRefreshRequested || isCacheExpired()) Maybe.empty() else getFromCache()
        return Maybe.concat(
            cacheMaybe,
            requestNetwork(isRefreshRequested)
        ).toObservable()
            .map { list ->
                list.filter { item ->
                    when (account) {
                        is AccountGroup -> {
                            account.includes(item.account)
                        }
                        is CryptoInterestAccount -> {
                            account.asset == (item as? CustodialInterestActivitySummaryItem)?.asset
                        }
                        else -> {
                            account == item.account
                        }
                    }
                }
            }.map { filteredList ->
                if (account is AllWalletsAccount) {
                    reconcileTransfersAndBuys(filteredList)
                } else {
                    filteredList
                }.sorted()
            }.map { filteredList ->
                if (account is AllWalletsAccount) {
                    reconcileCustodialAndInterestTxs(filteredList)
                } else {
                    filteredList
                }.sorted()
            }.map { list ->
                Timber.d("Activity list size: ${list.size}")
                val pruned = list.distinct()
                Timber.d("Activity list pruned size: ${pruned.size}")
                pruned
            }
    }

    private fun reconcileTransfersAndBuys(list: ActivitySummaryList): List<ActivitySummaryItem> {
        val custodialWalletActivity = list.filterIsInstance<CustodialTradingActivitySummaryItem>()
        val activityList = list.toMutableList()

        custodialWalletActivity.forEach { custodialItem ->

            val matchingItem = activityList.find { a ->
                a.txId == custodialItem.depositPaymentId
            } as? FiatActivitySummaryItem

            if (matchingItem?.type == TransactionType.DEPOSIT) {
                activityList.remove(matchingItem)
                transactionCache.remove(matchingItem)
            }
        }

        return activityList.toList().sorted()
    }

    private fun reconcileCustodialAndInterestTxs(list: ActivitySummaryList): List<ActivitySummaryItem> {
        val interestWalletActivity = list.filter {
            it.account is InterestAccount && it is CustodialInterestActivitySummaryItem
        }
        val activityList = list.toMutableList()

        interestWalletActivity.forEach { interestItem ->
            val matchingItem = activityList.find { a ->
                a.txId == interestItem.txId && a is CustodialTransferActivitySummaryItem
            } as? CustodialTransferActivitySummaryItem

            if (matchingItem?.type == TransactionType.DEPOSIT || matchingItem?.type == TransactionType.WITHDRAWAL) {
                activityList.remove(matchingItem)
                transactionCache.remove(matchingItem)
            }
        }

        return activityList.toList().sorted()
    }

    fun findCachedItem(asset: AssetInfo, txHash: String): ActivitySummaryItem? =
        transactionCache.filterIsInstance<CryptoActivitySummaryItem>().find {
            it.asset == asset && it.txId == txHash
        }

    fun findCachedItemById(txHash: String): ActivitySummaryItem? =
        transactionCache.find { it.txId == txHash }

    fun findCachedTradeItem(asset: AssetInfo, txHash: String): TradeActivitySummaryItem? =
        transactionCache.filterIsInstance<TradeActivitySummaryItem>().find {
            when (it.currencyPair) {
                is CurrencyPair.CryptoCurrencyPair -> {
                    val pair = it.currencyPair as CurrencyPair.CryptoCurrencyPair
                    pair.source == asset && it.txId == txHash
                }
                is CurrencyPair.CryptoToFiatCurrencyPair -> {
                    val pair = it.currencyPair as CurrencyPair.CryptoToFiatCurrencyPair
                    pair.source == asset && it.txId == txHash
                }
            }
        }

    fun findCachedItem(currency: String, txHash: String): FiatActivitySummaryItem? =
        transactionCache.filterIsInstance<FiatActivitySummaryItem>().find {
            it.currency == currency && it.txId == txHash
        }

    private fun requestNetwork(refreshRequested: Boolean): Maybe<ActivitySummaryList> {
        return if (refreshRequested || isCacheExpired()) {
            getFromNetwork()
        } else {
            Maybe.empty()
        }
    }

    override fun getFromNetwork(): Maybe<ActivitySummaryList> =
        coincore.allWallets()
            .flatMap { it.activity }
            .doOnSuccess { activityList ->
                // on error of activity returns onSuccess with empty list
                if (activityList.isNotEmpty()) {
                    transactionCache.clear()
                    transactionCache.addAll(activityList)
                }
                lastUpdatedTimestamp = System.currentTimeMillis()
            }.map { list ->
                // if network comes empty, but we have cache, return cache instead
                if (list.isEmpty() && transactionCache.isNotEmpty()) {
                    transactionCache
                } else {
                    list
                }
            }.toMaybe()

    override fun getFromCache(): Maybe<ActivitySummaryList> {
        return Maybe.just(transactionCache)
    }

    fun clear() {
        transactionCache.clear()
    }
}
