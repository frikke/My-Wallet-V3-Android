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
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.defaultFilter
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.core.user.WatchlistInfo
import com.blockchain.data.DataResource
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
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.androidcore.utils.extensions.zipSingles

class CoinViewInteractor(
    private val coincore: Coincore,
    private val tradeDataService: TradeDataService,
    private val currencyPrefs: CurrencyPrefs,
    private val dashboardPrefs: DashboardPrefs,
    private val identity: UserIdentity,
    private val walletModeService: WalletModeService,
    private val custodialWalletManager: CustodialWalletManager,
    private val assetActionsComparator: StateAwareActionsComparator,
    private val assetsManager: DynamicAssetsDataManager,
    private val watchlistDataManager: WatchlistDataManager,
) {
    private val walletMode: WalletMode
        get() = walletModeService.enabledWalletMode()

    fun loadAssetDetails(assetTicker: String): Single<Pair<CryptoAsset?, FiatCurrency>> =
        Single.just(Pair(coincore[assetTicker] as CryptoAsset, currencyPrefs.selectedFiatCurrency))

    fun loadAccountDetails(asset: CryptoAsset): Single<AssetInformation> =
        getAssetDisplayDetails(asset)

    fun loadHistoricPrices(
        asset: CryptoAsset,
        timeSpan: HistoricalTimeSpan
    ): Observable<DataResource<HistoricalRateList>> =
        asset.historicRateSeries(timeSpan).asObservable()

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
            custodialWalletManager.isCurrencyAvailableForTrading(asset.currency),
        ) { tier, sddEligible, buyAccess,
            sellAccess, depositCryptoAccess, isSupportedPair ->
            val custodialAccount = accountList.firstOrNull { it is CustodialTradingAccount }
            val nonCustodialAccount = accountList.firstOrNull { it is NonCustodialAccount }

            val isTradable = custodialAccount != null
            val canBuy = buyAccess is FeatureAccess.Granted

            val quickActions = when {
                isTradable && canBuy -> {
                    require(custodialAccount != null)
                    if (isSupportedPair) {
                        if (tier == Tier.GOLD || sddEligible) {
                            if (totalCryptoBalance[AssetFilter.Trading]?.isPositive == true) {
                                QuickActionData(
                                    startAction = QuickActionCta.Sell,
                                    endAction = QuickActionCta.Buy,
                                    actionableAccount = custodialAccount
                                )
                            } else {
                                QuickActionData(
                                    startAction = QuickActionCta.Receive,
                                    endAction = QuickActionCta.Buy,
                                    actionableAccount = custodialAccount
                                )
                            }
                        } else {
                            QuickActionData(
                                startAction = QuickActionCta.Receive,
                                endAction = QuickActionCta.Buy,
                                actionableAccount = custodialAccount
                            )
                        }
                    } else {
                        if (totalCryptoBalance[AssetFilter.Trading]?.isPositive == true) {
                            QuickActionData(
                                startAction = QuickActionCta.Receive,
                                endAction = QuickActionCta.Send,
                                actionableAccount = custodialAccount
                            )
                        } else {
                            QuickActionData(
                                startAction = QuickActionCta.Receive,
                                endAction = QuickActionCta.None,
                                actionableAccount = custodialAccount
                            )
                        }
                    }
                }
                isTradable && !canBuy -> {
                    require(custodialAccount != null)
                    if (isSupportedPair) {
                        QuickActionData(
                            startAction = QuickActionCta.Receive,
                            endAction = QuickActionCta.Buy,
                            actionableAccount = custodialAccount
                        )
                    } else {
                        QuickActionData(
                            startAction = QuickActionCta.Receive,
                            endAction = QuickActionCta.None,
                            actionableAccount = custodialAccount
                        )
                    }
                }
                nonCustodialAccount != null -> {
                    QuickActionData(
                        startAction = if (walletMode == WalletMode.UNIVERSAL) {
                            QuickActionCta.Receive
                        } else {
                            QuickActionCta.Swap
                        },
                        endAction = if (totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true) {
                            QuickActionCta.Send
                        } else {
                            QuickActionCta.None
                        },
                        actionableAccount = nonCustodialAccount
                    )
                }
                else -> {
                    QuickActionData(
                        startAction = QuickActionCta.None,
                        endAction = QuickActionCta.None,
                        actionableAccount = NullCryptoAccount()
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

        val accounts = asset.accountGroup(walletMode.defaultFilter())
            .map { it.accounts }
            .switchIfEmpty(Single.just(emptyList()))
            .flatMap { extractAccountDetails(it) }

        return Single.zip(
            accounts,
            load24hPriceDelta(asset),
            asset.interestRate(),
            watchlistDataManager.isAssetInWatchlist(asset.currency)
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
                val totalCryptoMoney = Money.zero(asset.currency)
                var totalCryptoMoneyAll = Money.zero(asset.currency)
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

        val accountComparator = object : Comparator<DetailsItem> {
            override fun compare(o1: DetailsItem, o2: DetailsItem): Int {
                return getAssignedValue(o1).compareTo(getAssignedValue(o2))
            }

            fun getAssignedValue(detailItem: DetailsItem): Int {
                return when {
                    detailItem.account is NonCustodialAccount && detailItem.isDefault -> 0
                    detailItem.account is TradingAccount -> 1
                    detailItem.account is InterestAccount -> 2
                    detailItem.account is NonCustodialAccount && detailItem.isDefault.not() -> 3
                    else -> Int.MAX_VALUE
                }
            }
        }

        val sortedAccounts = accounts.sortedWith(accountComparator)

        return sortedAccounts.map {
            when (walletMode) {
                WalletMode.UNIVERSAL,
                WalletMode.CUSTODIAL_ONLY -> {
                    AssetDisplayInfo.BrokerageDisplayInfo(
                        account = it.account,
                        filter = when (it.account) {
                            is TradingAccount -> AssetFilter.Trading
                            is InterestAccount -> AssetFilter.Interest
                            // todo (othman) should be removed once universal mode is removed
                            is NonCustodialAccount -> AssetFilter.NonCustodial
                            else -> error("account type not supported")
                        },
                        amount = it.balance,
                        fiatValue = exchangeRate.convert(it.balance),
                        pendingAmount = it.pendingBalance,
                        actions = it.actions.filter { action ->
                            action.action != AssetAction.InterestDeposit
                        }.toSet(),
                        interestRate = interestRate
                    )
                }
                WalletMode.NON_CUSTODIAL_ONLY -> {
                    AssetDisplayInfo.DefiDisplayInfo(
                        account = it.account,
                        amount = it.balance,
                        fiatValue = exchangeRate.convert(it.balance),
                        pendingAmount = it.pendingBalance,
                        actions = it.actions.filter { action ->
                            action.action != AssetAction.InterestDeposit
                        }.toSet()
                    )
                }
            }
        }
    }

    private fun extractAccountDetails(accounts: SingleAccountList): Single<List<DetailsItem>> =
        accounts.filter {
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
