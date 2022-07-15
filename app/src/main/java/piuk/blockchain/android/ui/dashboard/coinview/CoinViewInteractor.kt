package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.api.services.AssetTag
import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NonCustodialAccount
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
import com.blockchain.nabu.BlockedReason
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
            identity.userAccessForFeature(Feature.Buy),
            identity.userAccessForFeature(Feature.Sell),
            identity.userAccessForFeature(Feature.DepositCrypto),
            custodialWalletManager.isCurrencyAvailableForTrading(asset.assetInfo),
        ) { tier, sddEligible, buyAccess,
            sellAccess, depositCryptoAccess, isSupportedPair ->
            val custodialAccount = accountList.firstOrNull { it is CustodialTradingAccount }
            val ncAccount = accountList.firstOrNull { it is NonCustodialAccount }

            val isTradable = custodialAccount != null
            val canBuy = buyAccess is FeatureAccess.Granted

            val quickActions = when {
                isTradable && canBuy -> {
                    require(custodialAccount != null)
                    if (isSupportedPair) {
                        if (tier == Tier.GOLD || sddEligible) {
                            if (totalCryptoBalance[AssetFilter.Trading]?.isPositive == true) {
                                QuickActionData(QuickActionCta.Sell, QuickActionCta.Buy, custodialAccount)
                            } else {
                                QuickActionData(QuickActionCta.Receive, QuickActionCta.Buy, custodialAccount)
                            }
                        } else {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.Buy, custodialAccount)
                        }
                    } else {
                        if (totalCryptoBalance[AssetFilter.Trading]?.isPositive == true) {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.Send, custodialAccount)
                        } else {
                            QuickActionData(QuickActionCta.Receive, QuickActionCta.None, custodialAccount)
                        }
                    }
                }
                isTradable && !canBuy -> {
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

            quickActions.filterActions { action ->
                val isActionBlocked = when (action) {
                    QuickActionCta.Receive -> {
                        quickActions.actionableAccount == custodialAccount &&
                            depositCryptoAccess is FeatureAccess.Blocked
                    }
                    QuickActionCta.Sell -> sellAccess is FeatureAccess.Blocked
                    QuickActionCta.Buy ->
                        buyAccess is FeatureAccess.Blocked && buyAccess.reason !is BlockedReason.InsufficientTier
                    else -> false
                }
                !isActionBlocked
            }
        }

    private fun QuickActionData.filterActions(predicate: (QuickActionCta) -> Boolean): QuickActionData =
        this.copy(
            startAction = if (predicate(startAction)) startAction else QuickActionCta.None,
            endAction = if (predicate(endAction)) endAction else QuickActionCta.None
        )

    fun getAccountActions(account: BlockchainAccount): Single<CoinViewViewState> = Singles.zip(
        account.stateAwareActions,
        account.balance.firstOrError()
    ).map { (actions, balance) ->
        assetActionsComparator.initAccount(account, balance)
        val sortedActions = when (account) {
            is InterestAccount -> {
                if (actions.none { it.action == AssetAction.InterestDeposit }) {
                    actions + StateAwareAction(ActionState.Available, AssetAction.InterestDeposit)
                } else {
                    actions
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

        val accounts = coincore.walletsForAsset(asset).flatMap {
            extractAccountDetails(it)
        }

        return Single.zip(
            accounts,
            load24hPriceDelta(asset),
            asset.interestRate(),
            watchlistDataManager.isAssetInWatchlist(asset.assetInfo)
        ) { accounts, prices, interestRate, isAddedToWatchlist ->
            // while we wait for a BE flag on whether an asset is tradeable or not, we can check the
            // available accounts to see if we support custodial or PK balances as a guideline to asset support
            val tradeableAsset = accounts.any {
                it.account is NonCustodialAccount || it.account is CustodialTradingAccount
            }

            return@zip if (!tradeableAsset) {
                AssetInformation.NonTradeable(
                    isAddedToWatchlist = isAddedToWatchlist,
                    prices = prices
                )
            } else {
                val accountsList = mapAccounts(
                    accounts, prices.currentRate, interestRate
                )
                val totalCryptoMoney = Money.zero(asset.assetInfo)
                var totalCryptoMoneyAll = Money.zero(asset.assetInfo)
                val totalCryptoBalance = hashMapOf(AssetFilter.All to totalCryptoMoneyAll)
                var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

                accountsList.forEach { account ->
                    totalCryptoBalance[account.filter] = totalCryptoMoney.plus(account.amount)
                    totalCryptoMoneyAll = totalCryptoMoneyAll.plus(account.amount)
                    totalFiatBalance = totalFiatBalance.plus(account.fiatValue)
                }

                totalCryptoBalance[AssetFilter.All] = totalCryptoMoneyAll

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
        identity.userAccessForFeature(Feature.Buy)

    private fun mapAccounts(
        accounts: List<DetailsItem>,
        exchangeRate: ExchangeRate,
        interestRate: Double = Double.NaN
    ): List<AssetDisplayInfo> {
        val listOfAccounts = mutableListOf<AssetDisplayInfo>()

        listOfAccounts.addAll(
            accounts.filter { it.account is TradingAccount }.map {
                AssetDisplayInfo(
                    account = it.account,
                    filter = AssetFilter.Trading,
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
            accounts.filter { it.account is InterestAccount }.map {
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

        val ncLists = accounts.filter { it.account is NonCustodialAccount }.partition {
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

    private fun extractAccountDetails(accountGroup: AccountGroup): Single<List<DetailsItem>> =
        accountGroup.accounts.filter {
            (it as? CryptoNonCustodialAccount)?.isArchived?.not() ?: true
        }.map { account ->
            Single.zip(
                account.balance.firstOrError(),
                account.stateAwareActions
            ) { balance, actions ->
                DetailsItem(
                    account = account,
                    balance = balance.total,
                    pendingBalance = balance.pending,
                    actions = actions,
                    isDefault = account.isDefault
                )
            }
        }.zipSingles()
}
