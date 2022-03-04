package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.NullAccountGroup
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class CoinViewInteractor(
    private val coincore: Coincore,
    private val tradeDataManager: TradeDataManager,
    private val currencyPrefs: CurrencyPrefs
) {

    fun loadAssetDetails(assetTicker: String): Pair<CryptoAsset?, FiatCurrency> =
        Pair(coincore[assetTicker], currencyPrefs.selectedFiatCurrency)

    fun loadAccountDetails(asset: CryptoAsset): Single<AssetInformation> =
        getAssetDisplayDetails(asset)

    fun loadHistoricPrices(asset: CryptoAsset, timeSpan: HistoricalTimeSpan): Single<HistoricalRateList> =
        asset.historicRateSeries(timeSpan)
            .onErrorResumeNext { Single.just(emptyList()) }

    fun loadRecurringBuys(asset: AssetInfo): Single<List<RecurringBuy>> =
        tradeDataManager.getRecurringBuysForAsset(asset)

    private fun load24hPriceDelta(asset: CryptoAsset) =
        asset.getPricesWith24hDelta()

    private fun getAssetDisplayDetails(asset: CryptoAsset): Single<AssetInformation> {
        return Single.zip(
            splitAccountsInGroup(asset, AssetFilter.NonCustodial),
            load24hPriceDelta(asset),
            splitAccountsInGroup(asset, AssetFilter.Custodial),
            splitAccountsInGroup(asset, AssetFilter.Interest),
            asset.interestRate()
        ) { nonCustodialAccounts, prices, custodialAccounts, interestAccounts, interestRate ->
            val list =
                mapAccounts(nonCustodialAccounts, prices.currentRate, custodialAccounts, interestAccounts, interestRate)
            var totalCryptoBalance = Money.zero(asset.assetInfo)
            var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)
            list.forEach { account ->
                totalCryptoBalance = totalCryptoBalance.plus(account.amount)
                totalFiatBalance = totalFiatBalance.plus(account.fiatValue)
            }
            return@zip AssetInformation(prices, list, totalCryptoBalance as CryptoValue, totalFiatBalance as FiatValue)
        }
    }

    private fun mapAccounts(
        nonCustodialAccounts: List<Details.DetailsItem>,
        exchangeRate: ExchangeRate,
        custodialAccounts: List<Details.DetailsItem>,
        interestAccounts: List<Details.DetailsItem>,
        interestRate: Double = Double.NaN
    ): List<AssetDisplayInfo> {
        val listOfAccounts = mutableListOf<AssetDisplayInfo>()

        listOfAccounts.addAll(
            custodialAccounts.map {
                AssetDisplayInfo(
                    account = it.account,
                    filter = AssetFilter.Custodial,
                    amount = it.balance,
                    fiatValue = exchangeRate.convert(it.balance),
                    pendingAmount = it.pendingBalance,
                    actions = it.actions.filter { action ->
                        action.action != AssetAction.InterestDeposit
                    }.toSet(),
                    interestRate = interestRate
                )
            }
        )
        listOfAccounts.addAll(
            interestAccounts.map {
                AssetDisplayInfo(
                    account = it.account,
                    filter = AssetFilter.Interest,
                    amount = it.balance,
                    fiatValue = exchangeRate.convert(it.balance),
                    pendingAmount = it.pendingBalance,
                    actions = it.actions.filter { action ->
                        action.action != AssetAction.InterestDeposit
                    }.toSet(),
                    interestRate = interestRate
                )
            }
        )

        val ncLists = nonCustodialAccounts.partition {
            it.isDefault
        }

        listOfAccounts.addAll(
            0,
            ncLists.first.map {
                AssetDisplayInfo(
                    account = it.account,
                    filter = AssetFilter.NonCustodial,
                    amount = it.balance,
                    fiatValue = exchangeRate.convert(it.balance),
                    pendingAmount = it.pendingBalance,
                    actions = it.actions.filter { action ->
                        action.action != AssetAction.InterestDeposit
                    }.toSet(),
                    interestRate = interestRate
                )
            }
        )

        listOfAccounts.addAll(
            ncLists.second.map {
                AssetDisplayInfo(
                    account = it.account,
                    filter = AssetFilter.NonCustodial,
                    amount = it.balance,
                    fiatValue = exchangeRate.convert(it.balance),
                    pendingAmount = it.pendingBalance,
                    actions = it.actions.filter { action ->
                        action.action != AssetAction.InterestDeposit
                    }.toSet(),
                    interestRate = interestRate
                )
            }
        )

        return listOfAccounts
    }

    private fun splitAccountsInGroup(asset: CryptoAsset, filter: AssetFilter) =
        asset.accountGroup(filter).defaultIfEmpty(NullAccountGroup()).flatMap { accountGroup ->
            accountGroup.accounts.map { account ->
                Single.zip(
                    account.balance.firstOrError(),
                    account.isEnabled,
                    account.stateAwareActions
                ) { balance, enabled, actions ->
                    Details.DetailsItem(
                        isEnabled = enabled,
                        account = account,
                        balance = balance.total,
                        pendingBalance = balance.pending,
                        actions = actions,
                        isDefault = account.isDefault
                    )
                }
            }.zipSingles()
        }

    // converts a List<Single<Items>> -> Single<List<Items>>
    private fun <T> List<Single<T>>.zipSingles(): Single<List<T>> {
        if (this.isEmpty()) return Single.just(emptyList())
        return Single.zip(this) {
            @Suppress("UNCHECKED_CAST")
            return@zip (it as Array<T>).toList()
        }
    }
}
