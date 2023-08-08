package piuk.blockchain.android.ui.transfer

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountsSorter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface AccountsSorting {
    fun sorter(): AccountsSorter
}

class SwapSourceAccountsSorting(
    private val assetListOrderingFF: FeatureFlag,
    private val dashboardAccountsSorter: AccountsSorting,
    private val sellAccountsSorting: SellAccountsSorting,
) : AccountsSorting {
    override fun sorter(): AccountsSorter = { list ->
        assetListOrderingFF.enabled.flatMap { enabled ->
            if (enabled) {
                val sortedList = sellAccountsSorting.sorter().invoke(list)
                    .onErrorReturn { list }
                return@flatMap sortedList
            } else {
                val sortedList = dashboardAccountsSorter.sorter().invoke(list)
                    .onErrorReturn { list }
                return@flatMap sortedList
            }
        }
    }
}

class SwapTargetAccountsSorting(
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val watchlistDataManager: WatchlistDataManager
) : AccountsSorting {

    private data class AccountInfo(
        val account: SingleAccount,
        val totalBalance: Money,
        val tradingVolume: Double
    )

    override fun sorter(): AccountsSorter = { list ->
        val sortedList = Single.zip(
            Observable.fromIterable(list).flatMapSingle { account ->
                Single.zip(
                    account.balanceRx().firstOrError(),
                    exchangeRatesDataManager.getCurrentAssetPrice(
                        asset = account.currency,
                        fiat = currencyPrefs.selectedFiatCurrency
                    )
                ) { accountBalance, priceRecord ->
                    AccountInfo(
                        account = account,
                        totalBalance = ExchangeRate(
                            rate = priceRecord.rate,
                            from = account.currency,
                            to = currencyPrefs.selectedFiatCurrency
                        ).convert(accountBalance.total),
                        tradingVolume = priceRecord.tradingVolume24h ?: 0.0
                    )
                }
            }.toList(),
            watchlistDataManager.getWatchlist()
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

            return@zip sortedFinalAccounts.groupBy { it.account.currency.networkTicker }
                .mapValues { entry ->
                    entry.value.sortedWith(
                        compareByDescending<AccountInfo> { it.account is NonCustodialAccount }
                            .thenByDescending { it.totalBalance }
                            .thenByDescending { it.account.currency.index }
                    )
                }
                .values.flatten().map {
                    it.account
                }
        }.onErrorReturn { list }
        sortedList
    }
}

class SellAccountsSorting(
    private val coincore: Coincore
) : AccountsSorting {
    override fun sorter(): AccountsSorter = { accountList ->

        val sortedList = Observable.fromIterable(accountList).flatMapSingle { account ->
            coincore[account.currency].getPricesWith24hDeltaLegacy().flatMap { prices ->
                account.balanceRx().firstOrError().map { balance ->
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
            }.onErrorReturn {
                accountList
            }
        sortedList
    }
}

class DefaultAccountsSorting(private val currencyPrefs: CurrencyPrefs) : AccountsSorting {

    private data class AccountData(
        val totalBalance: Money?,
        val account: SingleAccount
    )

    override fun sorter(): AccountsSorter = { list ->
        Observable.fromIterable(list).flatMapSingle { account ->
            account.balanceRx().firstOrError().onErrorReturn {
                AccountBalance.zero(
                    currencyPrefs.selectedFiatCurrency,
                    ExchangeRate.zeroRateExchangeRate(currencyPrefs.selectedFiatCurrency)
                )
            }.map { balance ->
                AccountData(
                    totalBalance = balance.totalFiat,
                    account = account
                )
            }
        }.toList()
            .map { accountList ->
                accountList.sortedWith(
                    compareByDescending<AccountData> { it.totalBalance }
                        .thenByDescending { it.account.currency.index }
                        .thenByDescending { it.account.currency.displayTicker }
                )
                    .groupBy { it.account.currency.networkTicker }
                    .mapValues { entry ->
                        entry.value.sortedWith(
                            compareByDescending<AccountData> { it.account is NonCustodialAccount }
                                .thenByDescending { it.totalBalance }
                        )
                    }.values.flatten().map {
                        it.account
                    }
            }.onErrorResumeNext {
                Single.just(list)
            }
    }
}
