package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.NullAccountGroup
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.repositories.TradeDataManager

class CoinViewInteractor(
    private val coincore: Coincore,
    private val tradeDataManager: TradeDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val identity: UserIdentity
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

    fun loadQuickActions(asset: AssetInfo, totalCryptoBalance: Money): Single<Pair<QuickActionCta, QuickActionCta>> =
        Single.zip(
            identity.getHighestApprovedKycTier(),
            identity.isEligibleFor(Feature.SimplifiedDueDiligence)
        ) { tier, sddEligible ->
            val assetWithCustodialWallet = assetCatalogue.supportedCustodialAssets.firstOrNull { assetAccount ->
                assetAccount.networkTicker == asset.networkTicker && assetAccount.categories.contains(
                    AssetCategory.CUSTODIAL
                )
            }

            assetWithCustodialWallet?.let {
                if (tier == Tier.GOLD || sddEligible) {
                    if (totalCryptoBalance.isPositive) {
                        Pair(QuickActionCta.Sell, QuickActionCta.Buy)
                    } else {
                        Pair(QuickActionCta.Receive, QuickActionCta.Buy)
                    }
                } else {
                    Pair(QuickActionCta.Receive, QuickActionCta.Buy)
                }
            } ?: run {
                Pair(QuickActionCta.Receive, QuickActionCta.Send)
            }
        }

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
            // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
            // custodial endpoint products to see if we support custodial or PK balances as a guideline to
            // asset support
            val tradeableAsset = assetCatalogue.supportedCustodialAssets.firstOrNull { assetAccount ->
                assetAccount.networkTicker == asset.assetInfo.networkTicker &&
                    (
                        assetAccount.categories.contains(AssetCategory.CUSTODIAL) ||
                            assetAccount.categories.contains(AssetCategory.NON_CUSTODIAL)
                        )
            }

            return@zip if (tradeableAsset == null) {
                AssetInformation.NonTradeable(prices)
            } else {
                val list = mapAccounts(
                    nonCustodialAccounts, prices.currentRate, custodialAccounts, interestAccounts, interestRate
                )
                var totalCryptoBalance = Money.zero(asset.assetInfo)
                var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)
                list.forEach { account ->
                    totalCryptoBalance = totalCryptoBalance.plus(account.amount)
                    totalFiatBalance = totalFiatBalance.plus(account.fiatValue)
                }
                AssetInformation.AccountsInfo(
                    prices, list, totalCryptoBalance, totalFiatBalance
                )
            }
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
