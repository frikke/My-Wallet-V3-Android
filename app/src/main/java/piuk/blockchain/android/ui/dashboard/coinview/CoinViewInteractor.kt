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
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.core.user.WatchlistInfo
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.extensions.minus
import com.blockchain.extensions.replace
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.core.recurringbuy.RecurringBuy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.store.asSingle
import com.blockchain.utils.zipSingles
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator

class CoinViewInteractor(
    private val coincore: Coincore,
    private val tradeDataService: TradeDataService,
    private val currencyPrefs: CurrencyPrefs,
    private val dashboardPrefs: DashboardPrefs,
    private val identity: UserIdentity,
    private val kycService: KycService,
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
            tradeDataService.getRecurringBuysForAsset(asset, FreshnessStrategy.Fresh).asSingle(),
            custodialWalletManager.isCurrencyAvailableForTradingLegacy(asset)
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
    ): Single<QuickActionData> {
        return when (walletMode) {
            WalletMode.UNIVERSAL, WalletMode.CUSTODIAL_ONLY -> {
                Single.zip(
                    kycService.getHighestApprovedTierLevelLegacy(),
                    identity.isEligibleFor(Feature.SimplifiedDueDiligence),
                    identity.userAccessForFeature(Feature.Buy),
                    identity.userAccessForFeature(Feature.Sell),
                    custodialWalletManager.isCurrencyAvailableForTradingLegacy(asset.currency),
                    custodialWalletManager.isAssetSupportedForSwapLegacy(asset.currency)
                ) { kycTier, sddEligible, buyAccess, sellAccess, isSupportedPair, isSwapSupported ->

                    val custodialAccount = accountList.firstOrNull { it is CustodialTradingAccount }

                    val hasBalance = when (walletMode) {
                        WalletMode.UNIVERSAL -> totalCryptoBalance[AssetFilter.All]?.isPositive ?: false
                        WalletMode.CUSTODIAL_ONLY -> totalCryptoBalance[AssetFilter.Trading]?.isPositive ?: false
                        WalletMode.NON_CUSTODIAL_ONLY -> error("NON_CUSTODIAL_ONLY unreachable here")
                    }
                    /**
                     * Sell button will be enabled if
                     * * Sell access is [FeatureAccess.Granted]
                     *
                     * *AND*
                     *
                     * * Is available for trading ([isSupportedPair])
                     *
                     * *AND*
                     *
                     * * kyc Gold *OR* eligible for SimplifiedDueDiligence
                     */
                    val canSell = sellAccess is FeatureAccess.Granted &&
                        isSupportedPair &&
                        (kycTier == KycTier.GOLD || sddEligible) &&
                        hasBalance

                    /**
                     * Buy button will be enabled if
                     * * Is available for trading ([isSupportedPair])
                     *
                     * *AND*
                     *
                     * * Buy access is [FeatureAccess.Granted]
                     *
                     * *OR*
                     *
                     * * Access is [FeatureAccess.Blocked] but because of [BlockedReason.InsufficientTier],
                     * when trying to buy with low tier upgrading to gold will be requested
                     */
                    val canBuy = isSupportedPair && (
                        buyAccess is FeatureAccess.Granted ||
                            (buyAccess is FeatureAccess.Blocked && buyAccess.reason is BlockedReason.InsufficientTier)
                        )

                    /**
                     * Swap button will be enabled if
                     * * Balance is positive
                     */
                    val canSwap = hasBalance

                    custodialAccount?.let {
                        QuickActionData(
                            middleAction = if (isSwapSupported) QuickActionCta.Swap(canSwap) else QuickActionCta.None,
                            startAction = QuickActionCta.Sell(canSell),
                            endAction = QuickActionCta.Buy(canBuy),
                            actionableAccount = custodialAccount
                        )
                    } ?: QuickActionData(
                        middleAction = QuickActionCta.None,
                        startAction = QuickActionCta.None,
                        endAction = QuickActionCta.None,
                        actionableAccount = NullCryptoAccount()
                    )
                }
            }

            WalletMode.NON_CUSTODIAL_ONLY -> {
                custodialWalletManager.isAssetSupportedForSwapLegacy(asset.currency).map { isSwapSupported ->
                    val nonCustodialAccount = accountList.firstOrNull { it is NonCustodialAccount }

                    /**
                     * Send button will be enabled if
                     * * Balance is positive
                     */
                    val canSend = totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true

                    /**
                     * Can always receive
                     */
                    val canReceive = true

                    /**
                     * Swap button will be enabled if
                     * * Balance is positive
                     */
                    val canSwap = totalCryptoBalance[AssetFilter.NonCustodial]?.isPositive == true

                    nonCustodialAccount?.let {
                        QuickActionData(
                            middleAction = if (isSwapSupported) QuickActionCta.Swap(canSwap) else QuickActionCta.None,
                            startAction = QuickActionCta.Receive(canReceive),
                            endAction = QuickActionCta.Send(canSend),
                            actionableAccount = nonCustodialAccount
                        )
                    } ?: QuickActionData(
                        middleAction = QuickActionCta.None,
                        startAction = QuickActionCta.None,
                        endAction = QuickActionCta.None,
                        actionableAccount = NullCryptoAccount()
                    )
                }
            }
        }
    }

    private fun QuickActionData.filterActions(predicate: (QuickActionCta) -> Boolean): QuickActionData =
        this.copy(
            startAction = if (predicate(startAction)) startAction else QuickActionCta.None,
            endAction = if (predicate(endAction)) endAction else QuickActionCta.None
        )

    fun getAccountActions(asset: CryptoAsset, account: BlockchainAccount): Single<CoinViewViewState> = Singles.zip(
        account.stateAwareActions,
        custodialWalletManager.isCurrencyAvailableForTradingLegacy(asset.currency),
        account.balanceRx.firstOrError()
    ).map { (actions, isSupportedPair, balance) ->
        assetActionsComparator.initAccount(account, balance)

        // disable sell option if trading par not supported
        val actions = if (isSupportedPair.not() &&
            actions.any { it.action == AssetAction.Sell && it.state == ActionState.Available }
        ) {
            actions.replace(
                actions.first { it.action == AssetAction.Sell && it.state == ActionState.Available },
                StateAwareAction(ActionState.LockedDueToAvailability, AssetAction.Sell)
            ).toSet()
        } else {
            actions
        }

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
        asset.getPricesWith24hDeltaLegacy()

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
                var totalCryptoMoneyAll = Money.zero(asset.currency)
                val totalCryptoBalance = hashMapOf<AssetFilter, Money>()
                var totalFiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency)

                accountsList.forEach { account ->
                    totalCryptoBalance[account.filter] =
                        (totalCryptoBalance[account.filter] ?: Money.zero(asset.currency)).plus(account.amount)
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

    fun isBuyOptionAvailable(asset: CryptoAsset): Single<Boolean> =
        Single.zip(
            identity.userAccessForFeature(Feature.Buy),
            custodialWalletManager.isCurrencyAvailableForTradingLegacy(asset.currency),
        ) { buyAccess, isSupportedPair ->
            isSupportedPair && (
                buyAccess is FeatureAccess.Granted ||
                    (buyAccess is FeatureAccess.Blocked && buyAccess.reason is BlockedReason.InsufficientTier)
                )
        }

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
                account.balanceRx.firstOrError(),
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
