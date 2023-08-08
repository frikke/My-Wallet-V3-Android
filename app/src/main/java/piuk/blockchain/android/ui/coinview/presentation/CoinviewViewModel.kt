package piuk.blockchain.android.ui.coinview.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Star
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTierState
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.core.watchlist.domain.model.WatchlistToggle
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.doOnError
import com.blockchain.data.filterNotLoading
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.data.updateDataWith
import com.blockchain.domain.swap.SwapOption
import com.blockchain.image.LocalLogo
import com.blockchain.image.LogoValue
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.news.NewsService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.abbreviate
import com.blockchain.utils.toFormattedDateWithoutYear
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.Entry
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.coinview.domain.GetAccountActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetRecurringBuysUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadQuickActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickAction
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewRecurringBuys
import piuk.blockchain.android.ui.coinview.domain.model.isActiveRewardsAccount
import piuk.blockchain.android.ui.coinview.domain.model.isInterestAccount
import piuk.blockchain.android.ui.coinview.domain.model.isStakingAccount
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.CoinviewAccountState.Available
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.CoinviewAccountState.Unavailable
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.CoinviewRecurringBuyState
import timber.log.Timber

class CoinviewViewModel(
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val getAssetPriceUseCase: GetAssetPriceUseCase,
    private val watchlistService: WatchlistService,
    private val loadAssetAccountsUseCase: LoadAssetAccountsUseCase,
    private val getAccountActionsUseCase: GetAccountActionsUseCase,
    private val loadAssetRecurringBuysUseCase: LoadAssetRecurringBuysUseCase,
    private val loadQuickActionsUseCase: LoadQuickActionsUseCase,
    private val assetService: AssetService,
    private val custodialWalletManager: CustodialWalletManager,
    private val recurringBuyService: RecurringBuyService,
    private val newsService: NewsService,
    private val kycService: KycService
) : MviViewModel<
    CoinviewIntent,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    CoinviewArgs
    >(CoinviewModelState()) {

    companion object {
        private const val SNACKBAR_MESSAGE_DURATION: Long = 3000L
        private const val MAX_NEWS_COUNT = 5
    }

    private var loadPriceDataJob: Job? = null
    private var accountActionsJob: Job? = null
    private var loadWatchlistJob: Job? = null
    private var loadAccountsJob: Job? = null
    private var loadQuickActionsJob: Job? = null
    private var loadRecurringBuyJob: Job? = null
    private var loadAssetInfoJob: Job? = null
    private var loadNewsJob: Job? = null
    private var snackbarMessageJob: Job? = null
    private var pillAlertJob: Job? = null

    private val fiatCurrency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    private val defaultTimeSpan = HistoricalTimeSpan.DAY

    init {
        viewModelScope.launch(Dispatchers.IO) {
            kycService.stateFor(KycTier.GOLD).mapData { it == KycTierState.Rejected }
                .collectLatest {
                    updateState {
                        copy(
                            isKycRejected = it.dataOrElse(false)
                        )
                    }
                }
        }
    }

    override fun viewCreated(args: CoinviewArgs) {
        (coincore[args.networkTicker] as? CryptoAsset)?.let { asset ->
            updateState {
                copy(
                    asset = asset
                )
            }
        } ?: Timber.e("asset ${args.networkTicker} not found")

        args.recurringBuyId?.let {
            navigate(CoinviewNavigationEvent.ShowRecurringBuySheet(it))
        }
    }

    override fun CoinviewModelState.reduce() = CoinviewViewState(
        asset = reduceAsset(this),
        showKycRejected = walletMode == WalletMode.CUSTODIAL && isKycRejected,
        tradeable = reduceAssetTradeable(this),
        assetPrice = reduceAssetPrice(this),
        watchlist = reduceWatchlist(this),
        accounts = reduceAccounts(this),
        centerQuickAction = reduceCenterQuickActions(this),
        recurringBuys = reduceRecurringBuys(this),
        bottomQuickAction = reduceBottomQuickActions(this),
        assetInfo = reduceAssetInfo(this),
        news = reduceNews(),
        pillAlert = reducePillAlert(this),
        snackbarError = reduceSnackbarError(this)
    )

    private fun reduceAsset(
        state: CoinviewModelState
    ): DataResource<CoinviewAssetState> = state.run {
        if (asset == null) {
            DataResource.Error(Exception())
        } else {
            DataResource.Data(
                CoinviewAssetState(
                    asset = asset.currency,
                    l1Network = l1Network()
                )
            )
        }
    }

    private fun reduceAssetTradeable(state: CoinviewModelState): CoinviewAssetTradeableState = state.run {
        if (isTradeableAsset == false) {
            check(asset != null) { "asset not initialized" }

            CoinviewAssetTradeableState.NonTradeable(
                assetName = asset.currency.name,
                assetTicker = asset.currency.networkTicker
            )
        } else {
            CoinviewAssetTradeableState.Tradeable
        }
    }

    private fun reduceAssetPrice(state: CoinviewModelState): DataResource<CoinviewPriceState> = state.run {
        assetPriceHistory.map {
            // price, priceChange, percentChange
            // will contain values from interactiveAssetPrice to correspond with user interaction

            // intervalName will be empty if user is interacting with the chart

            check(asset != null) { "asset not initialized" }

            CoinviewPriceState(
                assetName = asset.currency.name,
                assetLogo = asset.currency.logo,
                fiatSymbol = fiatCurrency.symbol,
                price = (interactiveAssetPrice ?: it.priceDetail)
                    .price.toStringWithSymbol(),
                priceChange = (interactiveAssetPrice ?: it.priceDetail)
                    .changeDifference.toStringWithSymbol(),
                valueChange = calculateValueChange(
                    (
                        interactiveAssetPrice
                            ?: it.priceDetail
                        ).percentChange
                ),
                intervalText = if (interactiveAssetPrice != null) {
                    PriceIntervalText.Empty
                } else {
                    when (val tSpan = (it.priceDetail).timeSpan) {
                        HistoricalTimeSpan.DAY,
                        HistoricalTimeSpan.WEEK,
                        HistoricalTimeSpan.MONTH,
                        HistoricalTimeSpan.YEAR -> PriceIntervalText.TimeSpanText(tSpan)

                        HistoricalTimeSpan.ALL_TIME ->
                            asset.currency.startDate?.calculateTimePassed()?.let { period ->
                                PriceIntervalText.CustomPeriod(period)
                            } ?: PriceIntervalText.TimeSpanText(HistoricalTimeSpan.ALL_TIME)
                    }
                },
                chartData = when {
                    isChartDataLoading &&
                        requestedTimeSpan != null &&
                        it.priceDetail.timeSpan != requestedTimeSpan -> {
                        // show chart loading when data is loading and a new timespan is selected
                        CoinviewPriceState.CoinviewChartState.Loading
                    }

                    else -> CoinviewPriceState.CoinviewChartState.Data(
                        it.historicRates.map { point ->
                            ChartEntry(
                                point.timestamp.toFloat(),
                                point.rate.toFloat()
                            )
                        }
                    )
                },
                selectedTimeSpan = requestedTimeSpan ?: (interactiveAssetPrice ?: it.priceDetail).timeSpan
            )
        }
    }

    private fun Long.calculateTimePassed(): Period {
        val instant = Instant.ofEpochSecond(this)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val currentDate = LocalDateTime.now()
        return Period.between(dateTime.toLocalDate(), currentDate.toLocalDate())
    }

    private fun reduceWatchlist(
        state: CoinviewModelState
    ): DataResource<Boolean> = state.run {
        watchlist
    }

    private fun calculateValueChange(percentChange: Double): ValueChange =
        percentChange.takeIf { percent -> !percent.isNaN() && !percent.isInfinite() }
            ?.let {
                ValueChange.fromValue(BigDecimal(it).setScale(2, RoundingMode.FLOOR).toDouble())
            } ?: ValueChange.None(percentChange)

    private fun CoinviewModelState.l1Network(): CoinViewNetwork? {
        return asset?.let {
            if (walletMode == WalletMode.NON_CUSTODIAL) {
                asset.currency.takeIf { it.isLayer2Token }?.coinNetwork?.let {
                    CoinViewNetwork(
                        logo = assetCatalogue.fromNetworkTicker(it.nativeAssetTicker)?.logo.orEmpty(),
                        name = it.shortName
                    )
                }
            } else {
                null
            }
        }
    }

    private fun reduceAccounts(
        state: CoinviewModelState
    ): DataResource<CoinviewAccountsState?> = state.run {
        when (isTradeableAsset) {
            false -> {
                DataResource.Data(null)
            }

            else -> {
                assetDetail.map {
                    if (it is CoinviewAssetDetail.Tradeable) {
                        check(asset != null) { "reduceAccounts - asset not initialized" }

                        with(it) {
                            check(totalBalance.totalCryptoBalance.containsKey(AssetFilter.All)) {
                                "balance not initialized"
                            }

                            CoinviewAccountsState(
                                assetName = asset.currency.displayTicker,
                                totalBalance = totalBalance.totalFiatBalance?.toStringWithSymbol()
                                    ?: totalBalance.totalCryptoBalance[AssetFilter.All]?.toStringWithSymbol().orEmpty(),
                                accounts = accounts.accounts.map { cvAccount ->
                                    val account: CryptoAccount = cvAccount.account.let { blockchainAccount ->
                                        when (blockchainAccount) {
                                            is CryptoAccount -> blockchainAccount
                                            is AccountGroup -> blockchainAccount.selectFirstAccount()
                                            else -> throw IllegalStateException(
                                                "Unsupported account type for asset details ${cvAccount.account}"
                                            )
                                        }
                                    }

                                    when (cvAccount.isEnabled) {
                                        true -> {
                                            when (cvAccount) {
                                                is CoinviewAccount.Custodial.Trading ->
                                                    makeAvailableTradingAccount(cvAccount, asset)

                                                is CoinviewAccount.Custodial.Interest ->
                                                    makeAvailableInterestAccount(cvAccount, asset)

                                                is CoinviewAccount.Custodial.Staking ->
                                                    makeAvailableStakingAccount(cvAccount, asset)

                                                is CoinviewAccount.Custodial.ActiveRewards ->
                                                    makeAvailableActiveRewardsAccount(cvAccount, asset)

                                                is CoinviewAccount.PrivateKey ->
                                                    makeAvailablePrivateKeyAccount(
                                                        cvAccount, account, asset, l1Network()
                                                    )
                                            }
                                        }

                                        false -> {
                                            when (cvAccount) {
                                                is CoinviewAccount.Custodial.Trading ->
                                                    makeUnavailableTradingAccount(cvAccount, asset)

                                                is CoinviewAccount.Custodial.Interest ->
                                                    makeUnavailableInterestAccount(cvAccount)

                                                is CoinviewAccount.Custodial.Staking ->
                                                    makeUnavailableStakingAccount(cvAccount)

                                                is CoinviewAccount.Custodial.ActiveRewards ->
                                                    makeUnavailableActiveRewardsAccount(cvAccount)

                                                is CoinviewAccount.PrivateKey ->
                                                    makeUnavailablePrivateKeyAccount(cvAccount, account, l1Network())
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun makeUnavailablePrivateKeyAccount(
        cvAccount: CoinviewAccount,
        account: CryptoAccount,
        l1Network: CoinViewNetwork?
    ) = Unavailable(
        cvAccount = cvAccount,
        title = account.currency.name,
        subtitle = TextValue.IntResValue(com.blockchain.stringResources.R.string.coinview_nc_desc),
        logo = l1Network?.let { LogoValue.SmallTag(account.currency.logo, it.logo) }
            ?: LogoValue.SingleIcon(account.currency.logo)
    )

    private fun makeUnavailableStakingAccount(cvAccount: CoinviewAccount.Custodial.Staking) =
        Unavailable(
            cvAccount = cvAccount,
            title = labels.getDefaultStakingWalletLabel(),
            subtitle = TextValue.IntResValue(
                com.blockchain.stringResources.R.string.coinview_interest_no_balance,
                listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
            ),
            logo = LogoValue.SingleIcon(LocalLogo.StakingRewards)
        )

    private fun makeUnavailableActiveRewardsAccount(cvAccount: CoinviewAccount.Custodial.ActiveRewards) =
        Unavailable(
            cvAccount = cvAccount,
            title = labels.getDefaultActiveRewardsWalletLabel(),
            subtitle = TextValue.IntResValue(
                com.blockchain.stringResources.R.string.coinview_active_rewards_no_balance,
                listOf(DecimalFormat("0.#").format(cvAccount.activeRewardsRate))
            ),
            logo = LogoValue.SingleIcon(LocalLogo.ActiveRewards)
        )

    private fun makeUnavailableInterestAccount(cvAccount: CoinviewAccount.Custodial.Interest) =
        Unavailable(
            cvAccount = cvAccount,
            title = labels.getDefaultInterestWalletLabel(),
            subtitle = TextValue.IntResValue(
                com.blockchain.stringResources.R.string.coinview_interest_no_balance,
                listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
            ),
            logo = LogoValue.SingleIcon(LocalLogo.PassiveRewards)
        )

    private fun makeUnavailableTradingAccount(
        cvAccount: CoinviewAccount,
        asset: CryptoAsset
    ) = Unavailable(
        cvAccount = cvAccount,
        title = asset.currency.name,
        subtitle = TextValue.IntResValue(
            com.blockchain.stringResources.R.string.coinview_c_unavailable_desc,
            listOf(asset.currency.name)
        ),
        logo = LogoValue.SingleIcon(asset.currency.logo)
    )

    private fun makeAvailablePrivateKeyAccount(
        cvAccount: CoinviewAccount.PrivateKey,
        account: CryptoAccount,
        asset: CryptoAsset,
        l1Network: CoinViewNetwork?
    ) = Available(
        cvAccount = cvAccount,
        title = account.currency.name,
        subtitle = TextValue.StringValue(cvAccount.address.abbreviate(startLength = 4, endLength = 4)),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        logo = l1Network?.let { LogoValue.SmallTag(account.currency.logo, it.logo) }
            ?: LogoValue.SingleIcon(account.currency.logo),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableStakingAccount(
        cvAccount: CoinviewAccount.Custodial.Staking,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultStakingWalletLabel(),
        subtitle = TextValue.IntResValue(
            com.blockchain.stringResources.R.string.coinview_interest_with_balance,
            listOf(DecimalFormat("0.#").format(cvAccount.stakingRate))
        ),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        logo = LogoValue.SingleIcon(LocalLogo.StakingRewards),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableActiveRewardsAccount(
        cvAccount: CoinviewAccount.Custodial.ActiveRewards,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultActiveRewardsWalletLabel(),
        subtitle = TextValue.IntResValue(
            com.blockchain.stringResources.R.string.coinview_active_rewards_with_balance,
            listOf(DecimalFormat("0.#").format(cvAccount.activeRewardsRate))
        ),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        logo = LogoValue.SingleIcon(LocalLogo.ActiveRewards),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableInterestAccount(
        cvAccount: CoinviewAccount.Custodial.Interest,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = labels.getDefaultInterestWalletLabel(),
        subtitle = TextValue.IntResValue(
            com.blockchain.stringResources.R.string.coinview_interest_with_balance,
            listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
        ),
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        logo = LogoValue.SingleIcon(LocalLogo.PassiveRewards),
        assetColor = asset.currency.colour
    )

    private fun makeAvailableTradingAccount(
        cvAccount: CoinviewAccount,
        asset: CryptoAsset
    ) = Available(
        cvAccount = cvAccount,
        title = asset.currency.name,
        subtitle = null,
        cryptoBalance = cvAccount.cryptoBalance.map { it.toStringWithSymbol() }.dataOrElse(""),
        fiatBalance = cvAccount.fiatBalance.map { it?.toStringWithSymbol().orEmpty() }.dataOrElse(""),
        logo = LogoValue.SingleIcon(asset.currency.logo),
        assetColor = asset.currency.colour
    )

    private fun reduceRecurringBuys(
        state: CoinviewModelState
    ): DataResource<CoinviewRecurringBuysState> = state.run {
        recurringBuys.map {
            if (isTradeableAsset == false || walletMode == WalletMode.NON_CUSTODIAL) {
                // not supported for non custodial
                CoinviewRecurringBuysState.Data(emptyList())
            } else {
                check(asset != null) { "asset not initialized" }

                with(it) {
                    when {
                        data.isEmpty() && isAvailableForTrading -> {
                            CoinviewRecurringBuysState.Upsell
                        }

                        data.isEmpty() -> {
                            CoinviewRecurringBuysState.Data(emptyList())
                        }

                        else -> CoinviewRecurringBuysState.Data(
                            data.map { recurringBuy ->
                                CoinviewRecurringBuyState(
                                    id = recurringBuy.id,
                                    description = TextValue.IntResValue(
                                        com.blockchain.stringResources.R.string.dashboard_recurring_buy_item_title_1,
                                        listOf(
                                            recurringBuy.amount.toStringWithSymbol(),
                                            recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                                        )
                                    ),

                                    status = if (recurringBuy.state ==
                                        RecurringBuyState.ACTIVE
                                    ) {
                                        TextValue.IntResValue(
                                            com.blockchain.stringResources.R.string.dashboard_recurring_buy_item_label,
                                            listOf(recurringBuy.nextPaymentDate.toFormattedDateWithoutYear())
                                        )
                                    } else {
                                        TextValue.IntResValue(
                                            com.blockchain.stringResources
                                                .R.string.dashboard_recurring_buy_item_label_error
                                        )
                                    },

                                    assetColor = asset.currency.colour
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun reduceCenterQuickActions(
        state: CoinviewModelState
    ): DataResource<List<CoinviewQuickAction>> = state.run {
        when {
            isTradeableAsset == false -> {
                DataResource.Data(emptyList())
            }

            else -> {
                quickActions.map { it.center }
            }
        }
    }

    private fun reduceBottomQuickActions(
        state: CoinviewModelState
    ): DataResource<List<CoinviewQuickAction>> = state.run {
        when {
            isTradeableAsset == false -> {
                DataResource.Data(emptyList())
            }

            else -> {
                quickActions.map { it.bottom }
            }
        }
    }

    private fun reduceAssetInfo(state: CoinviewModelState): CoinviewAssetInfoState = state.run {
        when (assetInfo) {
            DataResource.Loading -> {
                CoinviewAssetInfoState.Loading
            }

            is DataResource.Error -> {
                CoinviewAssetInfoState.Error
            }

            is DataResource.Data -> {
                require(asset != null) { "asset not initialized" }

                with(assetInfo.data) {
                    CoinviewAssetInfoState.Data(
                        assetName = asset.currency.name,
                        description = if (description.isEmpty().not()) description else null,
                        website = if (website.isEmpty().not()) website else null
                    )
                }
            }
        }
    }

    private fun CoinviewModelState.reduceNews() = CoinviewNewsState(
        newsArticles = newsArticles.dataOrElse(emptyList())
    )

    private fun reducePillAlert(state: CoinviewModelState): CoinviewPillAlertState = state.run {
        when (state.alert) {
            CoinviewPillAlert.WatchlistAdded -> CoinviewPillAlertState.Alert(
                message = com.blockchain.stringResources.R.string.coinview_added_watchlist,
                icon = Icons.Filled.Star.withTint(Color(0XFFFFCD53))
            )

            CoinviewPillAlert.None -> CoinviewPillAlertState.None
        }.also {
            // reset to None
            if (it != CoinviewPillAlertState.None) {
                pillAlertJob?.cancel()
                pillAlertJob = viewModelScope.launch {
                    delay(SNACKBAR_MESSAGE_DURATION)

                    updateState {
                        copy(alert = CoinviewPillAlert.None)
                    }
                }
            }
        }
    }

    private fun reduceSnackbarError(state: CoinviewModelState): CoinviewSnackbarAlertState = state.run {
        when (state.error) {
            CoinviewError.AccountsLoadError -> CoinviewSnackbarAlertState.AccountsLoadError
            CoinviewError.ActionsLoadError -> CoinviewSnackbarAlertState.ActionsLoadError
            CoinviewError.WatchlistToggleError -> CoinviewSnackbarAlertState.WatchlistToggleError
            CoinviewError.None -> CoinviewSnackbarAlertState.None
        }.also {
            // reset to None
            if (it != CoinviewSnackbarAlertState.None) {
                snackbarMessageJob?.cancel()
                snackbarMessageJob = viewModelScope.launch {
                    delay(SNACKBAR_MESSAGE_DURATION)

                    updateState {
                        copy(error = CoinviewError.None)
                    }
                }
            }
        }
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntent) {
        when (intent) {
            is CoinviewIntent.LoadAllData -> {
                check(modelState.asset != null) { "LoadAllData asset not initialized" }
                val walletMode = walletModeService.walletMode.first()
                updateState {
                    copy(
                        walletMode = walletMode
                    )
                }

                onIntent(CoinviewIntent.LoadPriceData)
                onIntent(CoinviewIntent.LoadAccountsData)
                onIntent(CoinviewIntent.LoadWatchlistData)
                onIntent(CoinviewIntent.LoadAssetInfo)
                onIntent(CoinviewIntent.LoadNews)
            }

            CoinviewIntent.LoadPriceData -> {
                check(modelState.asset != null) { "LoadPriceData asset not initialized" }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = (modelState.assetPriceHistory as? DataResource.Data)
                        ?.data?.priceDetail?.timeSpan ?: defaultTimeSpan
                )
            }

            CoinviewIntent.LoadWatchlistData -> {
                require(modelState.asset != null) { "asset not initialized" }

                loadWatchlistData(
                    asset = modelState.asset
                )
            }

            CoinviewIntent.LoadAccountsData -> {
                check(modelState.asset != null) { "LoadAccountsData asset not initialized" }

                loadAccountsData(
                    asset = modelState.asset
                )
            }

            CoinviewIntent.RecurringBuyDeleted -> {
                check(modelState.asset != null) { "LoadRecurringBuysData asset not initialized" }

                loadRecurringBuysData(modelState.asset, FreshnessStrategy.Fresh)
            }

            is CoinviewIntent.LoadQuickActions -> {
                check(modelState.asset != null) { "LoadQuickActions asset not initialized" }

                loadQuickActionsData(
                    asset = modelState.asset,
                    accounts = intent.accounts,
                    totalBalance = intent.totalBalance
                )
            }

            CoinviewIntent.LoadAssetInfo -> {
                check(modelState.asset != null) { "LoadAssetInfo asset not initialized" }

                loadAssetInformation(
                    asset = modelState.asset
                )
            }

            CoinviewIntent.LoadNews -> {
                check(modelState.asset != null) { "LoadAssetInfo asset not initialized" }

                loadNews(
                    asset = modelState.asset
                )
            }

            is CoinviewIntent.UpdatePriceForChartSelection -> {
                check(
                    modelState.assetPriceHistory is DataResource.Data
                ) { "UpdatePriceForChartSelection price data not initialized" }

                updatePriceForChartSelection(
                    entry = intent.entry,
                    assetPriceHistory = modelState.assetPriceHistory.data
                )
            }

            is CoinviewIntent.ResetPriceSelection -> {
                resetPriceSelection()
            }

            is CoinviewIntent.NewTimeSpanSelected -> {
                check(modelState.asset != null) { "NewTimeSpanSelected asset not initialized" }

                updateState { copy(requestedTimeSpan = intent.timeSpan) }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = intent.timeSpan
                )
            }

            CoinviewIntent.ToggleWatchlist -> {
                check(modelState.asset != null) { "asset not initialized" }
                check(modelState.watchlist is DataResource.Data) { "watchlist not initialized" }

                updateWatchlist(
                    asset = modelState.asset,
                    toggle = if (modelState.watchlist.data) WatchlistToggle.REMOVE else WatchlistToggle.ADD
                )
            }

            is CoinviewIntent.AccountSelected -> {
                check(modelState.asset != null) { "AccountSelected asset not initialized" }
                require(intent.account.cryptoBalance is DataResource.Data)
                require(intent.account.fiatBalance is DataResource.Data)
                handleAccountSelected(
                    account = intent.account,
                    asset = modelState.asset
                )
            }

            is CoinviewIntent.AccountExplainerAcknowledged -> {
                check(modelState.accounts != null) { "AccountExplainerAcknowledged accounts not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }
                val balance = cvAccount.cryptoBalance as? DataResource.Data ?: return
                val fiatBalance = cvAccount.fiatBalance as? DataResource.Data ?: return

                when {
                    cvAccount.isInterestAccount() -> navigate(
                        CoinviewNavigationEvent.NavigateToInterestStatement(cvAccount)
                    )

                    cvAccount.isStakingAccount() -> navigate(
                        CoinviewNavigationEvent.NavigateToStakingStatement(cvAccount)
                    )

                    cvAccount.isActiveRewardsAccount() -> navigate(
                        CoinviewNavigationEvent.NavigateToActiveRewardsStatement(cvAccount)
                    )

                    else -> navigate(
                        CoinviewNavigationEvent.ShowAccountActions(
                            cvAccount = cvAccount,
                            interestRate = cvAccount.interestRate(),
                            stakingRate = cvAccount.stakingRate(),
                            activeRewardsRate = cvAccount.activeRewardsRate(),
                            actions = intent.actions,
                            cryptoBalance = balance.data,
                            fiatBalance = fiatBalance.data
                        )
                    )
                }
            }

            is CoinviewIntent.AccountActionSelected -> {
                require(modelState.asset != null) { "AccountActionSelected asset not initialized" }
                require(modelState.accounts != null) { "AccountActionSelected accounts not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }

                handleAccountActionSelected(
                    account = cvAccount,
                    asset = modelState.asset,
                    action = intent.action
                )
            }

            is CoinviewIntent.NoBalanceUpsell -> {
                require(modelState.accounts != null) { "NoBalanceUpsell accounts not initialized" }
                require(modelState.asset != null) { "NoBalanceUpsell asset not initialized" }

                val cvAccount = modelState.accounts!!.accounts.first { it.account == intent.account }

                custodialWalletManager.isCurrencyAvailableForTrading(
                    modelState.asset.currency
                )
                    .filterNotLoading()
                    .doOnData { availableToBuy ->
                        navigate(
                            CoinviewNavigationEvent.ShowNoBalanceUpsell(
                                cvAccount,
                                intent.action,
                                availableToBuy
                            )
                        )
                    }.firstOrNull()
            }

            CoinviewIntent.LockedAccountSelected -> {
                navigate(
                    CoinviewNavigationEvent.ShowKycUpgrade
                )
            }

            CoinviewIntent.RecurringBuysUpsell -> {
                require(modelState.asset != null) { "RecurringBuysUpsell asset not initialized" }

                navigate(
                    if (hasAnyAssetsWithRecurringBuy()) {
                        CoinviewNavigationEvent.NavigateToBuy(
                            asset = modelState.asset,
                            fromRecurringBuy = true
                        )
                    } else {
                        CoinviewNavigationEvent.NavigateToRecurringBuyUpsell(
                            asset = modelState.asset
                        )
                    }
                )
            }

            is CoinviewIntent.ShowRecurringBuyDetail -> {
                navigate(
                    CoinviewNavigationEvent.ShowRecurringBuyInfo(
                        recurringBuyId = intent.recurringBuyId
                    )
                )
            }

            is CoinviewIntent.QuickActionSelected -> {
                require(modelState.asset != null) { "QuickActionSelected asset not initialized" }

                when (intent.quickAction) {
                    is CoinviewQuickAction.Buy -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToBuy(
                                asset = modelState.asset,
                                fromRecurringBuy = false
                            )
                        )
                    }

                    is CoinviewQuickAction.Sell -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSell(
                                cvAccount = modelState.actionableAccount()
                            )
                        )
                    }

                    is CoinviewQuickAction.Send -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSend(
                                cvAccount = modelState.actionableAccount()
                            )
                        )
                    }

                    is CoinviewQuickAction.Receive -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToReceive(
                                cvAccount = modelState.actionableAccount(isPositiveBalanceRequired = false),
                                isBuyReceive = (modelState.quickActions.map { it.canBuy() } as? DataResource.Data)
                                    ?.data ?: false,
                                isSendReceive = (modelState.quickActions.map { it.canSend() } as? DataResource.Data)
                                    ?.data ?: false
                            )
                        )
                    }

                    is CoinviewQuickAction.Swap -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSwap(
                                cvAccount = modelState.actionableAccount(),
                                swapOption = intent.quickAction.swapOption
                            )
                        )
                    }

                    is CoinviewQuickAction.Get -> {
                        navigate(
                            CoinviewNavigationEvent.NavigateToSwap(
                                cvAccount = modelState.actionableAccount(isPositiveBalanceRequired = false),
                                swapOption = intent.quickAction.swapOption
                            )
                        )
                    }
                }
            }

            CoinviewIntent.ContactSupport -> {
                navigate(CoinviewNavigationEvent.NavigateToSupport)
            }

            CoinviewIntent.VisitAssetWebsite -> {
                check(modelState.assetInfo is DataResource.Data) { "assetInfo not initialized" }
                navigate(CoinviewNavigationEvent.OpenAssetWebsite(modelState.assetInfo.data.website))
            }

            is CoinviewIntent.LaunchStakingDepositFlow ->
                navigate(CoinviewNavigationEvent.NavigateToStakingDeposit(getStakingAccount(modelState.accounts)))

            is CoinviewIntent.LaunchStakingWithdrawFlow ->
                navigate(
                    CoinviewNavigationEvent.NavigateToStakingWithdraw(
                        cvSourceStakingAccount = getStakingAccount(modelState.accounts),
                        cvTargetCustodialTradingAccount = getCustodialTradingAccount(modelState.accounts)
                    )
                )

            is CoinviewIntent.LaunchStakingActivity ->
                navigate(CoinviewNavigationEvent.NavigateToActivity(getStakingAccount(modelState.accounts)))

            is CoinviewIntent.LaunchActiveRewardsDepositFlow ->
                navigate(
                    CoinviewNavigationEvent.NavigateToActiveRewardsDeposit(
                        getActiveRewardsAccount(modelState.accounts)
                    )
                )

            is CoinviewIntent.LaunchActiveRewardsWithdrawFlow ->
                navigate(
                    CoinviewNavigationEvent.NavigateToActiveRewardsWithdraw(
                        cvSourceActiveRewardsAccount = getActiveRewardsAccount(modelState.accounts),
                        cvTargetCustodialTradingAccount = getCustodialTradingAccount(modelState.accounts)
                    )
                )
        }
    }

    private fun getStakingAccount(accounts: CoinviewAccounts?): CoinviewAccount {
        require(accounts != null) { "getStakingAccount - accounts not initialized" }
        require(accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.Staking>().isNotEmpty()) {
            "getStakingAccount no staking account source found"
        }

        return accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.Staking>().first()
    }

    private fun getActiveRewardsAccount(accounts: CoinviewAccounts?): CoinviewAccount {
        require(accounts != null) { "getActiveRewardsAccount - accounts not initialized" }
        require(accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.ActiveRewards>().isNotEmpty()) {
            "getActiveRewardsAccount no active rewards account source found"
        }

        return accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.ActiveRewards>().first()
    }

    private fun getCustodialTradingAccount(accounts: CoinviewAccounts?): CoinviewAccount {
        require(accounts != null) { "getCustodialTradingAccount - accounts not initialized" }
        require(accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.Trading>().isNotEmpty()) {
            "getCustodialTradingAccount no custodial trading account source found"
        }

        return accounts.accounts.filterIsInstance<CoinviewAccount.Custodial.Trading>().first()
    }

    // //////////////////////
    // Prices
    private fun loadPriceData(
        asset: CryptoAsset,
        requestedTimeSpan: HistoricalTimeSpan
    ) {
        loadPriceDataJob?.cancel()
        loadPriceDataJob = viewModelScope.launch {
            getAssetPriceUseCase(
                asset = asset,
                timeSpan = requestedTimeSpan,
                fiatCurrency = fiatCurrency
            ).collectLatest { dataResource ->
                when (dataResource) {
                    DataResource.Loading -> {
                        updateState {
                            copy(
                                isChartDataLoading = true,
                                assetPriceHistory = assetPriceHistory.updateDataWith(dataResource)
                            )
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            copy(
                                isChartDataLoading = false,
                                assetPriceHistory = dataResource
                            )
                        }
                    }

                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                copy(
                                    isChartDataLoading = false,
                                    assetPriceHistory = DataResource.Error(Exception("no historicRates"))
                                )
                            }
                        } else {
                            updateState {
                                copy(
                                    isChartDataLoading = false,
                                    assetPriceHistory = dataResource,
                                    requestedTimeSpan = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Build a [CoinviewModelState.interactiveAssetPrice] based on the [entry] selected
     * to update the price information with
     */
    private fun updatePriceForChartSelection(
        entry: Entry,
        assetPriceHistory: CoinviewAssetPriceHistory
    ) {
        val historicRates = assetPriceHistory.historicRates
        historicRates.firstOrNull { it.timestamp.toFloat() == entry.x }?.let { selectedHistoricalRate ->
            val firstForPeriod = historicRates.first()
            val difference = selectedHistoricalRate.rate - firstForPeriod.rate

            val percentChange = (difference / firstForPeriod.rate)

            val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

            updateState {
                copy(
                    interactiveAssetPrice = CoinviewAssetPrice(
                        price = Money.fromMajor(
                            fiatCurrency,
                            selectedHistoricalRate.rate.toBigDecimal()
                        ),
                        timeSpan = assetPriceHistory.priceDetail.timeSpan,
                        changeDifference = changeDifference,
                        percentChange = percentChange
                    )
                )
            }
        }
    }

    /**
     * Reset [CoinviewModelState.interactiveAssetPrice] to update the price information with original value
     */
    private fun resetPriceSelection() {
        updateState { copy(interactiveAssetPrice = null) }
    }

    // //////////////////////
    // Watchlist
    private fun loadWatchlistData(asset: CryptoAsset) {
        loadWatchlistJob?.cancel()
        loadWatchlistJob = viewModelScope.launch {
            watchlistService.isAssetInWatchlist(asset.currency).collectLatest { dataResource ->
                updateState {
                    copy(
                        watchlist = watchlist.updateDataWith(dataResource)
                    )
                }
            }
        }
    }

    private fun updateWatchlist(asset: CryptoAsset, toggle: WatchlistToggle) {
        viewModelScope.launch {
            watchlistService.updateWatchlist(
                asset = asset.currency,
                toggle = toggle
            ).let { dataResource ->
                when (dataResource) {
                    is DataResource.Error -> {
                        updateState {
                            copy(error = CoinviewError.WatchlistToggleError)
                        }
                    }

                    is DataResource.Data -> {
                        loadWatchlistData(
                            asset = asset
                        )

                        updateState {
                            copy(
                                alert = if (toggle == WatchlistToggle.ADD) {
                                    CoinviewPillAlert.WatchlistAdded
                                } else {
                                    CoinviewPillAlert.None
                                }
                            )
                        }
                    }

                    DataResource.Loading -> {
                        // n/a
                    }
                }
            }
        }
    }

    // //////////////////////
    // Accounts
    /**
     * Loads accounts and total balance
     */
    private fun loadAccountsData(asset: CryptoAsset) {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            loadAssetAccountsUseCase(asset = asset).collectLatest { dataResource ->

                updateState {
                    copy(
                        assetDetail = assetDetail.updateDataWith(dataResource),
                        // on failure - fail quick actions too
                        // on data - load quick actions /see below
                        quickActions = if (dataResource is DataResource.Error) {
                            DataResource.Error(dataResource.error)
                        } else {
                            quickActions
                        },
                        error = if (dataResource is DataResource.Error) {
                            CoinviewError.AccountsLoadError
                        } else {
                            error
                        }
                    )
                }

                if (dataResource is DataResource.Data) {
                    when (dataResource.data) {
                        is CoinviewAssetDetail.Tradeable -> {
                            // now that we got accounts and it's a tradeable asset
                            // -> get quick actions
                            with(dataResource.data as CoinviewAssetDetail.Tradeable) {
                                onIntent(
                                    CoinviewIntent.LoadQuickActions(
                                        accounts = accounts,
                                        totalBalance = totalBalance
                                    )
                                )
                            }
                            modelState.asset?.let {
                                loadRecurringBuysData(it)
                            }
                        }

                        is CoinviewAssetDetail.NonTradeable -> {
                            updateState {
                                copy(
                                    quickActions = DataResource.Data(CoinviewQuickActions.none()),
                                    recurringBuys = DataResource.Data(CoinviewRecurringBuys(emptyList(), false))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAccountSelected(account: CoinviewAccount, asset: CryptoAsset) {
        accountActionsJob?.cancel()
        accountActionsJob = viewModelScope.launch {
            getAccountActionsUseCase(account)
                .doOnData { actions ->
                    getAccountActionsUseCase.getSeenAccountExplainerState(account).let { (hasSeen, markAsSeen) ->
                        if (hasSeen.not()) {
                            // show explainer
                            navigate(
                                CoinviewNavigationEvent.ShowAccountExplainer(
                                    cvAccount = account,
                                    networkTicker = asset.currency.networkTicker,
                                    interestRate = account.interestRate(),
                                    stakingRate = account.stakingRate(),
                                    activeRewardsRate = account.activeRewardsRate(),
                                    actions = actions
                                )
                            )
                            markAsSeen()
                        } else {
                            when {
                                account.isInterestAccount() -> navigate(
                                    CoinviewNavigationEvent.NavigateToInterestStatement(account)
                                )

                                account.isStakingAccount() -> navigate(
                                    CoinviewNavigationEvent.NavigateToStakingStatement(account)
                                )

                                account.isActiveRewardsAccount() -> navigate(
                                    CoinviewNavigationEvent.NavigateToActiveRewardsStatement(account)
                                )

                                else -> navigate(
                                    CoinviewNavigationEvent.ShowAccountActions(
                                        cvAccount = account,
                                        interestRate = account.interestRate(),
                                        stakingRate = account.stakingRate(),
                                        activeRewardsRate = account.activeRewardsRate(),
                                        actions = actions,
                                        cryptoBalance = (account.cryptoBalance as DataResource.Data).data,
                                        fiatBalance = (account.fiatBalance as DataResource.Data).data
                                    )
                                )
                            }
                        }
                    }
                }
                .doOnError {
                    updateState {
                        copy(error = CoinviewError.ActionsLoadError)
                    }
                }
        }
    }

    private fun CoinviewAccount.interestRate(): Double {
        val noInterestRate = 0.0
        return when (this) {
            is CoinviewAccount.Custodial.Interest -> {
                interestRate
            }

            else -> {
                noInterestRate
            }
        }
    }

    private fun CoinviewAccount.stakingRate(): Double {
        val noStakingRate = 0.0
        return when (this) {
            is CoinviewAccount.Custodial.Staking -> {
                stakingRate
            }

            else -> {
                noStakingRate
            }
        }
    }

    private fun CoinviewAccount.activeRewardsRate(): Double {
        val noActiveRewardsRate = 0.0
        return when (this) {
            is CoinviewAccount.Custodial.ActiveRewards -> {
                activeRewardsRate
            }

            else -> {
                noActiveRewardsRate
            }
        }
    }

    private fun handleAccountActionSelected(
        account: CoinviewAccount,
        asset: CryptoAsset,
        action: AssetAction
    ) {
        when (action) {
            AssetAction.Send -> navigate(
                CoinviewNavigationEvent.NavigateToSend(
                    cvAccount = account
                )
            )

            AssetAction.Receive -> navigate(
                CoinviewNavigationEvent.NavigateToReceive(
                    cvAccount = account,
                    isBuyReceive = (modelState.quickActions.map { it.canBuy() } as? DataResource.Data)
                        ?.data ?: false,
                    isSendReceive = (modelState.quickActions.map { it.canSend() } as? DataResource.Data)
                        ?.data ?: false
                )
            )

            AssetAction.Sell -> navigate(
                CoinviewNavigationEvent.NavigateToSell(
                    cvAccount = account
                )
            )

            AssetAction.Buy -> navigate(
                CoinviewNavigationEvent.NavigateToBuy(
                    asset = asset,
                    fromRecurringBuy = false
                )
            )

            AssetAction.Swap -> navigate(
                CoinviewNavigationEvent.NavigateToSwap(
                    cvAccount = account,
                    swapOption = SwapOption.BcdcSwap
                )
            )

            AssetAction.ViewActivity -> navigate(
                CoinviewNavigationEvent.NavigateToActivity(
                    cvAccount = account
                )
            )

            AssetAction.ViewStatement -> navigate(
                when {
                    account.isInterestAccount() -> {
                        CoinviewNavigationEvent.NavigateToInterestStatement(
                            cvAccount = account
                        )
                    }

                    account.isStakingAccount() -> {
                        CoinviewNavigationEvent.NavigateToStakingStatement(
                            cvAccount = account
                        )
                    }

                    account.isActiveRewardsAccount() -> {
                        CoinviewNavigationEvent.NavigateToActiveRewardsStatement(
                            cvAccount = account
                        )
                    }

                    else -> throw IllegalStateException("ViewStatement is not supported for account $account")
                }
            )

            AssetAction.InterestDeposit,
            AssetAction.InterestWithdraw,
            AssetAction.StakingDeposit,
            AssetAction.ActiveRewardsDeposit,
            AssetAction.ActiveRewardsWithdraw -> {
                // no-op
            }

            else -> throw IllegalStateException("Action $action is not supported in this flow")
        }
    }

    // //////////////////////
    // Recurring buys
    private fun loadRecurringBuysData(
        asset: CryptoAsset,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ) {
        loadRecurringBuyJob?.cancel()
        loadRecurringBuyJob = viewModelScope.launch {
            loadAssetRecurringBuysUseCase(asset, freshnessStrategy).collectLatest { dataResource ->
                updateState {
                    copy(recurringBuys = recurringBuys.updateDataWith(dataResource))
                }
            }
        }
    }

    private suspend fun hasAnyAssetsWithRecurringBuy(): Boolean {
        return recurringBuyService.recurringBuys(
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .filterNotLoading()
            .mapData { it.isNotEmpty() }
            .firstOrNull()
            ?.dataOrElse(false) ?: false
    }

    // //////////////////////
    // Quick actions
    private fun loadQuickActionsData(
        asset: CryptoAsset,
        accounts: CoinviewAccounts,
        totalBalance: CoinviewAssetTotalBalance
    ) {
        loadQuickActionsJob?.cancel()
        loadQuickActionsJob = viewModelScope.launch {
            loadQuickActionsUseCase(
                asset = asset,
                accounts = accounts,
                totalBalance = totalBalance
            ).collectLatest { dataResource ->
                updateState {
                    copy(
                        quickActions = quickActions.updateDataWith(dataResource)
                    )
                }
            }
        }
    }

    private fun CoinviewQuickActions.canBuy(): Boolean {
        return actions.any { it is CoinviewQuickAction.Buy }
    }

    private fun CoinviewQuickActions.canSend(): Boolean {
        return actions.any { it is CoinviewQuickAction.Send }
    }

    // //////////////////////
    // Asset info
    private fun loadAssetInformation(asset: CryptoAsset) {
        loadAssetInfoJob?.cancel()
        loadAssetInfoJob = viewModelScope.launch {
            assetService.getAssetInformation(asset = asset.currency).collectLatest { dataResource ->
                updateState {
                    copy(
                        assetInfo = assetInfo.updateDataWith(dataResource)
                    )
                }
            }
        }
    }

    // //////////////////////
    // News
    private fun loadNews(asset: CryptoAsset) {
        loadNewsJob?.cancel()
        loadNewsJob = viewModelScope.launch {
            newsService.articles(
                tickers = listOf(asset.currency.networkTicker)
            ).mapData {
                it.take(MAX_NEWS_COUNT)
            }.collectLatest {
                updateState {
                    copy(
                        newsArticles = newsArticles.updateDataWith(it)
                    )
                }
            }
        }
    }
}

sealed class PriceIntervalText {
    object Empty : PriceIntervalText()
    data class TimeSpanText(val historicalTimeSpan: HistoricalTimeSpan) : PriceIntervalText()
    data class CustomPeriod(val period: Period) : PriceIntervalText()
}
