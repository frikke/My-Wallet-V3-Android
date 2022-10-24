package piuk.blockchain.android.ui.transfer

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountsSorter
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.user.Watchlist
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.ui.brokerage.buy.PriceHistory
import piuk.blockchain.android.ui.brokerage.buy.PricedAsset

interface AccountsSorting {
    fun sorter(): AccountsSorter
}

class SwapSourceAccountsSorting(
    private val assetListOrderingFF: FeatureFlag,
    private val dashboardAccountsSorter: AccountsSorting,
    private val sellAccountsSorting: SellAccountsSorting,
    private val momentLogger: MomentLogger
) : AccountsSorting {
    override fun sorter(): AccountsSorter = { list ->
        assetListOrderingFF.enabled.flatMap { enabled ->
            if (enabled) {
                momentLogger.startEvent(MomentEvent.SWAP_SOURCE_LIST_FF_ON)
                val sortedList = sellAccountsSorting.sorter().invoke(list)
                    .onErrorReturn { list }
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SWAP_SOURCE_LIST_FF_ON)
                }
            } else {
                momentLogger.startEvent(MomentEvent.SWAP_SOURCE_LIST_FF_OFF)
                val sortedList = dashboardAccountsSorter.sorter().invoke(list)
                    .onErrorReturn { list }
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SWAP_SOURCE_LIST_FF_OFF)
                }
            }
        }
    }
}

class SwapTargetAccountsSorting(
    private val assetListOrderingFF: FeatureFlag,
    private val dashboardAccountsSorter: AccountsSorting,
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val watchlistDataManager: WatchlistDataManager,
    private val momentLogger: MomentLogger
) : AccountsSorting {

    private data class AccountInfo(
        val account: SingleAccount,
        val totalBalance: Money,
        val tradingVolume: Double
    )

    override fun sorter(): AccountsSorter = { list ->
        assetListOrderingFF.enabled.flatMap { enabled ->
            if (enabled) {
                momentLogger.startEvent(MomentEvent.SWAP_TARGET_LIST_FF_ON)
                val sortedList = Single.zip(
                    Observable.fromIterable(list).flatMapSingle { account ->
                        Single.zip(
                            account.balanceRx.firstOrError(),
                            coincore[account.currency].getPricesWith24hDeltaLegacy(),
                            exchangeRatesDataManager.getCurrentAssetPrice(account.currency, FiatCurrency.Dollars)
                        ) { accountBalance, pricesHistory, priceRecord ->
                            AccountInfo(
                                account = account,
                                totalBalance = pricesHistory.currentRate.convert(accountBalance.total),
                                tradingVolume = priceRecord.tradingVolume24h ?: 0.0
                            )
                        }
                    }.toList(),
                    watchlistDataManager.getWatchlist(),
                ) { accountInfoItems, watchlist ->
                    val sortedAccountsInWatchlist = watchlist.assetMap.keys
                        .mapNotNull { currency ->
                            accountInfoItems.find { it.account.currency == currency }
                        }.sortedWith(
                            compareByDescending<AccountInfo> {
                                it.totalBalance
                            }.thenByDescending { it.tradingVolume }
                        )

                    val sortedAvailableAccounts = accountInfoItems.sortedWith(
                        compareByDescending<AccountInfo> {
                            it.totalBalance
                        }.thenByDescending { it.tradingVolume }
                    )

                    val sortedFinalAccounts = sortedAccountsInWatchlist.toSet() + sortedAvailableAccounts.toSet()

                    return@zip sortedFinalAccounts.map {
                        it.account
                    }
                }.onErrorReturn { list }

                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SWAP_TARGET_LIST_FF_ON)
                }
            } else {
                momentLogger.startEvent(MomentEvent.SWAP_TARGET_LIST_FF_OFF)
                val sortedList = dashboardAccountsSorter.sorter().invoke(list)
                    .onErrorReturn { list }

                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SWAP_TARGET_LIST_FF_OFF)
                }
            }
        }
    }
}

class SellAccountsSorting(
    private val assetListOrderingFF: FeatureFlag,
    private val dashboardAccountsSorter: AccountsSorting,
    private val coincore: Coincore,
    private val momentLogger: MomentLogger
) : AccountsSorting {
    override fun sorter(): AccountsSorter = { accountList ->
        assetListOrderingFF.enabled.flatMap { enabled ->
            if (enabled) {
                momentLogger.startEvent(MomentEvent.SELL_LIST_FF_ON)
                val sortedList = Observable.fromIterable(accountList).flatMapSingle { account ->
                    coincore[account.currency].getPricesWith24hDeltaLegacy().flatMap { prices ->
                        account.balanceRx.firstOrError().map { balance ->
                            Pair(account, prices.currentRate.convert(balance.total))
                        }
                    }
                }.toList()
                    .flatMap { list ->
                        val groupedList = list.groupBy { (account, _) ->
                            account.currency.networkTicker
                        }

                        val sortedGroups = groupedList.values.map { group ->
                            group.sortedByDescending { (_, balance) ->
                                balance
                            }
                        }.sortedByDescending { sortedGroup ->
                            sortedGroup.first().second
                        }.flatten().map { (account, _) ->
                            account
                        }

                        Single.just(sortedGroups)
                    }.onErrorReturn { accountList }
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SELL_LIST_FF_ON)
                }
            } else {
                momentLogger.startEvent(MomentEvent.SELL_LIST_FF_OFF)
                val sortedList = dashboardAccountsSorter.sorter().invoke(accountList)
                    .onErrorReturn { accountList }
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.SELL_LIST_FF_OFF)
                }
            }
        }
    }
}

class DefaultAccountsSorting(
    private val dashboardPrefs: DashboardPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val momentLogger: MomentLogger
) : AccountsSorting {

    private data class AccountData(
        val totalBalance: Money,
        val account: SingleAccount
    )

    override fun sorter(): AccountsSorter = { list ->
        if (walletModeService.enabledWalletMode() != WalletMode.CUSTODIAL_ONLY) {
            momentLogger.startEvent(MomentEvent.DEFAULT_SORTING_NC_AND_UNIVERSAL)
            val sortedList = universalOrdering(list)
            sortedList.doFinally {
                momentLogger.endEvent(MomentEvent.DEFAULT_SORTING_NC_AND_UNIVERSAL)
            }
        } else {
            momentLogger.startEvent(MomentEvent.DEFAULT_SORTING_CUSTODIAL_ONLY)
            val sortedList = Observable.fromIterable(list).flatMapSingle { account ->
                Single.zip(
                    account.balanceRx.firstOrError(),
                    coincore[account.currency].getPricesWith24hDeltaLegacy(),
                ) { balance, prices ->
                    AccountData(
                        totalBalance = prices.currentRate.convert(balance.total),
                        account = account
                    )
                }
            }.toList()
                .map { accountList ->
                    accountList.sortedByDescending { it.totalBalance }
                        .map { accountData ->
                            accountData.account
                        }
                }
            sortedList.doFinally {
                momentLogger.endEvent(MomentEvent.DEFAULT_SORTING_CUSTODIAL_ONLY)
            }
        }
    }

    private fun universalOrdering(list: List<SingleAccount>) =
        Single.fromCallable { getOrdering() }
            .map { orderedAssets ->
                val sortedList = list.sortedWith(
                    compareBy(
                        {
                            (it as? CryptoAccount)?.let { cryptoAccount ->
                                orderedAssets.indexOf(cryptoAccount.currency)
                            } ?: 0
                        },
                        { it !is NonCustodialAccount },
                        { !it.isDefault }
                    )
                )
                sortedList
            }

    private fun getOrdering(): List<AssetInfo> =
        dashboardPrefs.dashboardAssetOrder
            .takeIf { it.isNotEmpty() }?.let {
                it.mapNotNull { ticker -> assetCatalogue.assetInfoFromNetworkTicker(ticker) }
            } ?: assetCatalogue.supportedCryptoAssets.sortedBy { it.displayTicker }
}

class BuyListAccountSorting(
    private val assetListOrderingFF: FeatureFlag,
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val watchlistDataManager: WatchlistDataManager,
    private val momentLogger: MomentLogger
) {

    fun sort(assets: List<AssetInfo>): Single<List<PricedAsset>> =
        // we will used the FF to act as an A/B test between ordering assets and what comes from BE
        assetListOrderingFF.enabled.flatMap { enabled ->
            if (enabled) {
                momentLogger.startEvent(MomentEvent.BUY_LIST_ORDERING_FF_ON)
                val sortedList = getAssetListOrdering(assets)
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.BUY_LIST_ORDERING_FF_ON)
                }
            } else {
                momentLogger.startEvent(MomentEvent.BUY_LIST_ORDERING_FF_OFF)
                val sortedList = Observable.fromIterable(assets).flatMapMaybe { asset ->
                    asset.getAssetPriceInformation()
                }.toList()
                return@flatMap sortedList.doFinally {
                    momentLogger.endEvent(MomentEvent.BUY_LIST_ORDERING_FF_OFF)
                }
            }
        }

    private fun getAssetListOrdering(assets: List<AssetInfo>): Single<List<PricedAsset>> =
        Single.zip(
            Observable.fromIterable(assets).flatMapMaybe { asset ->
                Maybe.zip(
                    coincore[asset].accountGroup(AssetFilter.All).flatMap {
                        it.balanceRx.firstOrError().toMaybe()
                    }.onErrorReturn { AccountBalance.zero(asset) },
                    asset.getAssetPriceInformation(),
                    // trading volumes are only returned in USD, so request them in that fiat here
                    exchangeRatesDataManager.getCurrentAssetPrice(asset, FiatCurrency.Dollars)
                        .toMaybe()
                ) { accountBalance, pricedAsset, priceRecord ->
                    PricedAsset.SortedAsset(
                        asset = pricedAsset.asset,
                        balance = pricedAsset.priceHistory.currentExchangeRate.convert(accountBalance.total),
                        priceHistory = pricedAsset.priceHistory,
                        tradingVolume = priceRecord.tradingVolume24h ?: 0.0
                    )
                }
            }.toList(),
            watchlistDataManager.getWatchlist().onErrorReturn { Watchlist(emptyMap()) },
        ) { items, watchlist ->

            val sortedAccountsInWatchlist = watchlist.assetMap.keys
                .mapNotNull { currency ->
                    items.find { it.asset == currency }
                }.sortedWith(
                    compareByDescending<PricedAsset.SortedAsset> {
                        it.balance
                    }.thenByDescending { it.tradingVolume }
                )

            val sortedAvailableAccounts = items.sortedWith(
                compareByDescending<PricedAsset.SortedAsset> {
                    it.balance
                }.thenByDescending { it.tradingVolume }
            )

            val sortedFinalAccounts = sortedAccountsInWatchlist.toSet() + sortedAvailableAccounts.toSet()

            return@zip sortedFinalAccounts.toList()
        }

    private fun AssetInfo.getAssetPriceInformation(): Maybe<PricedAsset.NonSortedAsset> =
        coincore[this].getPricesWith24hDeltaLegacy().map { priceDelta ->
            PricedAsset.NonSortedAsset(
                asset = this,
                priceHistory = PriceHistory(
                    currentExchangeRate = priceDelta.currentRate,
                    priceDelta = priceDelta.delta24h
                )
            )
        }.toMaybe().onErrorResumeNext {
            Maybe.empty()
        }
}
