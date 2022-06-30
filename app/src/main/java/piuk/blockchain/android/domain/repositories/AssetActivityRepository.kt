package piuk.blockchain.android.domain.repositories

import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.core.common.caching.ExpiringRepository
import com.blockchain.nabu.datamanagers.TransactionType
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import timber.log.Timber

class AssetActivityRepository : ExpiringRepository<ActivitySummaryList, BlockchainAccount>() {

    private val transactionCache = mutableListOf<ActivitySummaryItem>()

    fun fetch(
        account: BlockchainAccount,
        isRefreshRequested: Boolean,
    ): Single<ActivitySummaryList> {
        val cacheMaybe = if (isRefreshRequested || isCacheExpired()) Maybe.empty() else getFromCache(account)
        return cacheMaybe.defaultIfEmpty(emptyList()).flatMap {
            if (it.isEmpty())
                requestNetwork(account).defaultIfEmpty(emptyList())
            else Single.just(it)
        }.map { list ->
            list.filter { item ->
                when (account) {
                    is AccountGroup -> {
                        account.includes(item.account)
                    }
                    is CryptoInterestAccount -> {
                        account.currency == (item as? CustodialInterestActivitySummaryItem)?.asset
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

    private fun reconcileTransfersAndBuys(
        list: ActivitySummaryList,
    ): List<ActivitySummaryItem> {
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

    private fun reconcileCustodialAndInterestTxs(
        list: ActivitySummaryList,
    ): List<ActivitySummaryItem> {
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
            it.currencyPair.source == asset && it.txId == txHash
        }

    fun findCachedItem(currency: FiatCurrency, txHash: String): FiatActivitySummaryItem? =
        transactionCache.filterIsInstance<FiatActivitySummaryItem>().find {
            it.currency == currency && it.txId == txHash
        }

    private fun requestNetwork(account: BlockchainAccount): Maybe<ActivitySummaryList> {
        return getFromNetwork(account)
    }

    override fun getFromNetwork(param: BlockchainAccount): Maybe<ActivitySummaryList> =
        param.activity
            .doOnSuccess { activityList ->
                if (activityList.isNotEmpty()) {
                    transactionCache.clear()
                    transactionCache.addAll(activityList)
                }
                lastUpdatedTimestamp = System.currentTimeMillis()
            }.map { list ->
                list
            }.toMaybe()

    override fun getFromCache(param: BlockchainAccount): Maybe<ActivitySummaryList> {
        return when (param) {
            is AccountGroup -> getCachedActivitiesOfAccountGroup(param)
            is SingleAccount -> getCachedActivitiesOfSingleAccount(param)
            else -> Maybe.empty()
        }
    }

    private fun getCachedActivitiesOfSingleAccount(param: SingleAccount): Maybe<List<ActivitySummaryItem>> {
        val list = transactionCache.filter { it.account == param }
        list.takeIf { it.isNotEmpty() }?.let {
            return Maybe.just(it)
        } ?: return Maybe.empty()
    }

    private fun getCachedActivitiesOfAccountGroup(param: AccountGroup): Maybe<List<ActivitySummaryItem>> {
        val list = transactionCache.filter { param.accounts.contains(it.account) }
        list.takeIf { it.isNotEmpty() }?.let {
            return Maybe.just(it)
        } ?: return Maybe.empty()
    }

    fun clear() {
        transactionCache.clear()
    }
}
