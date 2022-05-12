package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.api.services.AssetTag
import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NullAccountGroup
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.core.user.WatchlistInfo
import com.blockchain.extensions.minus
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.androidcore.utils.extensions.zipSingles

class CoinViewInteractor(
    private val coincore: Coincore,
    private val tradeDataService: TradeDataService,
    private val currencyPrefs: CurrencyPrefs,
    private val dashboardPrefs: DashboardPrefs,
    private val identity: UserIdentity,
    private val custodialWalletManager: CustodialWalletManager,
    private val assetActionsComparator: StateAwareActionsComparator,
    private val assetsManager: DynamicAssetsDataManager,
    private val watchlistDataManager: WatchlistDataManager,
) {

    fun loadAssetDetails(assetTicker: String): Single<Pair<CryptoAsset?, FiatCurrency>> =
        Single.just(Pair(coincore[assetTicker], currencyPrefs.selectedFiatCurrency))

    fun loadAccountDetails(asset: CryptoAsset): Single<AssetInformation> =
        getAssetDisplayDetails(asset)

    fun loadHistoricPrices(asset: CryptoAsset, timeSpan: HistoricalTimeSpan): Single<HistoricalRateList> =
        asset.historicRateSeries(timeSpan)
            .onErrorResumeNext { Single.just(emptyList()) }

    fun loadAssetInformation(asset: AssetInfo): Single<DetailedAssetInformation> =
        assetsManager.getAssetInformation(asset)

    fun loadRecurringBuys(asset: AssetInfo): Single<Pair<List<RecurringBuy>, Boolean>> =
        Single.zip(
            tradeDataService.getRecurringBuysForAsset(asset),
            custodialWalletManager.isCurrencyAvailableForTrading(asset)
        ) { rbList, isSupportedPair ->
            Pair(rbList, isSupportedPair)
        }

    fun removeFromWatchlist(asset: Currency): Completable =
        watchlistDataManager.removeFromWatchList(asset, listOf(AssetTag.Favourite))

    fun addToWatchlist(asset: Currency): Single<WatchlistInfo> =
        watchlistDataManager.addToWatchlist(asset, listOf(AssetTag.Favourite))

    fun loadQuickActions(
        totalCryptoBalance: Map<AssetFilter, Money>,
        accountList: List<BlockchainAccount>,
        asset: CryptoAsset
    ): Single<QuickActionData> =
        Single.zip(
            identity.getHighestApprovedKycTier(),
            identity.isEligibleFor(Feature.SimplifiedDueDiligence),
            identity.userAccessForFeature(Feature.SimpleBuy),
            identity.userAccessForFeature(Feature.Buy),
            custodialWalletManager.isCurrencyAvailableForTrading(asset.assetInfo),
        ) { tier, sddEligible, simpleBuyAccess, buyAccess, isSupportedPair ->
            val custodialAccount = accountList.firstOrNull { it is CustodialTradingAccount }
            val ncAccount = accountList.firstOrNull { it is NonCustodialAccount }

            val isTradable = custodialAccount != null
            val canTrade = simpleBuyAccess is FeatureAccess.Granted && buyAccess is FeatureAccess.Granted

            when {
                isTradable && canTrade -> {
                    require(custodialAccount != null)
                    if (isSupportedPair) {
                        if (tier == Tier.GOLD || sddEligible) {
                            if (totalCryptoBalance[AssetFilter.Custodial]?.isPositive == true) {
                                QuickActionData(QuickActionCta.Sell, QuickActionCta.Buy, custodialAccount)
                            } else {
                                QuickActionData(QuickActionCta.Receive, QuickActionCta.Buy, custodialAccount)
                            }
                        } else {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.Buy, custodialAccount)
                        }
                    } else {
                        if (totalCryptoBalance[AssetFilter.Custodial]?.isPositive == true) {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.Send, custodialAccount)
                        } else {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.None, custodialAccount)
                        }
                    }
                }
                isTradable && !canTrade -> {
                    require(custodialAccount != null)
                    if (isSupportedPair) {
                        QuickActionData(QuickActionCta.Receive, QuickActionCta.Buy, custodialAccount)
                    } else {
                        QuickActionData(QuickActionCta.Receive, QuickActionCta.None, custodialAccount)
                    }
                }
                ncAccount != null -> {
                    QuickActionData(
                        QuickActionCta.Receive,
                        if (totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true) {
                            QuickActionCta.Send
                        } else {
                            QuickActionCta.None
                        },
                        ncAccount
                    )
                }
                else -> {
                    QuickActionData(
                        QuickActionCta.None,
                        QuickActionCta.None,
                        NullCryptoAccount()
                    )
                }
            }
        }

    internal fun getAccountActions(account: BlockchainAccount): Single<CoinViewViewState> = Singles.zip(
        account.stateAwareActions,
        account.isEnabled,
        account.balance.firstOrError()
    ).map { (actions, enabled, balance) ->
        assetActionsComparator.initAccount(account, balance)
        val sortedActions = when (account) {
            is InterestAccount -> {
                when {
                    !enabled && account.isFunded -> {
                        actions.minus { it.action == AssetAction.InterestDeposit } +
                            StateAwareAction(ActionState.Available, AssetAction.InterestWithdraw)
                    }
                    else -> {
                        actions + StateAwareAction(ActionState.Available, AssetAction.InterestDeposit)
                    }
                }
            }
            else -> actions.minus { it.action == AssetAction.InterestDeposit }
        }.sortedWith(assetActionsComparator).toTypedArray()
        return@map if (checkShouldShowExplainerSheet(account)) {
            CoinViewViewState.ShowAccountExplainerSheet(sortedActions)
        } else {
            CoinViewViewState.ShowAccountActionSheet(sortedActions)
        }
    }

    private fun checkShouldShowExplainerSheet(selectedAccount: BlockchainAccount): Boolean {
        return when (selectedAccount) {
            is NonCustodialAccount -> {
                if (dashboardPrefs.isPrivateKeyIntroSeen) {
                    false
                } else {
                    dashboardPrefs.isPrivateKeyIntroSeen = true
                    true
                }
            }
            is TradingAccount -> {
                if (dashboardPrefs.isCustodialIntroSeen) {
                    false
                } else {
                    dashboardPrefs.isCustodialIntroSeen = true
                    true
                }
            }
            is InterestAccount -> {
                if (dashboardPrefs.isRewardsIntroSeen) {
                    false
                } else {
                    dashboardPrefs.isRewardsIntroSeen = true
                    true
                }
            }
            else -> true
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
            asset.interestRate(),
            watchlistDataManager.isAssetInWatchlist(asset.assetInfo)
        ) { nonCustodialAccounts, prices, custodialAccounts, interestAccounts, interestRate, isAddedToWatchlist ->
            // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
            // available accounts to see if we support custodial or PK balances as a guideline to asset support
            val tradeableAsset = nonCustodialAccounts.isNotEmpty() || custodialAccounts.isNotEmpty()

            return@zip if (!tradeableAsset) {
                AssetInformation.NonTradeable(
                    isAddedToWatchlist = isAddedToWatchlist,
                    prices = prices
                )
            } else {
                val accountsList = mapAccounts(
                    nonCustodialAccounts, prices.currentRate, custodialAccounts, interestAccounts, interestRate
                )
                val totalCryptoMoney = Money.zero(asset.assetInfo)
                val totalCryptoBalance = hashMapOf(AssetFilter.All to totalCryptoMoney)
                var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

                accountsList.forEach { account ->
                    totalCryptoBalance[account.filter] = totalCryptoMoney.plus(account.amount)
                    totalCryptoBalance[AssetFilter.All]?.plus(account.amount)
                    totalFiatBalance = totalFiatBalance.plus(account.fiatValue)
                }
                AssetInformation.AccountsInfo(
                    isAddedToWatchlist = isAddedToWatchlist,
                    prices = prices,
                    accountsList = accountsList,
                    totalCryptoBalance = totalCryptoBalance,
                    totalFiatBalance = totalFiatBalance
                )
            }
        }
    }

    fun checkIfUserCanBuy(): Single<FeatureAccess> =
        identity.userAccessForFeature(Feature.SimpleBuy)

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
            accountGroup.accounts.filter {
                if (filter == AssetFilter.NonCustodial) {
                    !(it as CryptoNonCustodialAccount).isArchived
                } else {
                    true
                }
            }.map { account ->
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
}
