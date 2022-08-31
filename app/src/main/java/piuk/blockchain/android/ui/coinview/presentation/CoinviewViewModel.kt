package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.eth.MultiChainAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.utils.toFormattedDateWithoutYear
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.github.mikephil.charting.data.Entry
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.text.DecimalFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetRecurringBuysUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetInformation
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Available
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Unavailable
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountsHeaderState
import piuk.blockchain.android.ui.coinview.presentation.CoinviewRecurringBuysState.Data.RecurringBuyState

class CoinviewViewModel(
    walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val getAssetPriceUseCase: GetAssetPriceUseCase,
    private val loadAssetAccountsUseCase: LoadAssetAccountsUseCase,
    private val loadAssetRecurringBuysUseCase: LoadAssetRecurringBuysUseCase
) : MviViewModel<
    CoinviewIntents,
    CoinviewViewState,
    CoinviewModelState,
    CoinviewNavigationEvent,
    CoinviewArgs>(CoinviewModelState(walletMode = walletModeService.enabledWalletMode())) {

    private var loadPriceDataJob: Job? = null

    private val fiatCurrency: FiatCurrency
        get() = currencyPrefs.selectedFiatCurrency

    private val defaultTimeSpan = HistoricalTimeSpan.DAY

    override fun viewCreated(args: CoinviewArgs) {
        (coincore[args.networkTicker] as? CryptoAsset)?.let { asset ->
            updateState {
                it.copy(
                    asset = asset
                )
            }
        } ?: error("asset ${args.networkTicker} not found")
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState = state.run {
        CoinviewViewState(
            assetName = asset?.currency?.name ?: "",
            assetPrice = reduceAssetPrice(this),
            totalBalance = reduceTotalBalance(this),
            accounts = reduceAccounts(this),
            recurringBuys = reduceRecurringBuys(this)
        )
    }

    private fun reduceAssetPrice(state: CoinviewModelState): CoinviewPriceState = state.run {
        when (assetPriceHistory) {
            DataResource.Loading -> {
                CoinviewPriceState.Loading
            }

            is DataResource.Error -> {
                CoinviewPriceState.Error
            }

            is DataResource.Data -> {
                // price, priceChange, percentChange
                // will contain values from interactiveAssetPrice to correspond with user interaction

                // intervalName will be empty if user is interacting with the chart

                require(asset != null) { "asset not initialized" }

                with(assetPriceHistory.data) {
                    CoinviewPriceState.Data(
                        assetName = asset.currency.name,
                        assetLogo = asset.currency.logo,
                        fiatSymbol = fiatCurrency.symbol,
                        price = (interactiveAssetPrice ?: priceDetail)
                            .price.toStringWithSymbol(),
                        priceChange = (interactiveAssetPrice ?: priceDetail)
                            .changeDifference.toStringWithSymbol(),
                        percentChange = (interactiveAssetPrice ?: priceDetail).percentChange,
                        intervalName = if (interactiveAssetPrice != null) R.string.empty else
                            when ((priceDetail).timeSpan) {
                                HistoricalTimeSpan.DAY -> R.string.coinview_price_day
                                HistoricalTimeSpan.WEEK -> R.string.coinview_price_week
                                HistoricalTimeSpan.MONTH -> R.string.coinview_price_month
                                HistoricalTimeSpan.YEAR -> R.string.coinview_price_year
                                HistoricalTimeSpan.ALL_TIME -> R.string.coinview_price_all
                            },
                        chartData = when {
                            isChartDataLoading &&
                                requestedTimeSpan != null &&
                                priceDetail.timeSpan != requestedTimeSpan -> {
                                // show chart loading when data is loading and a new timespan is selected
                                CoinviewPriceState.Data.CoinviewChart.Loading
                            }
                            else -> CoinviewPriceState.Data.CoinviewChart.Data(
                                historicRates.map { point ->
                                    ChartEntry(
                                        point.timestamp.toFloat(),
                                        point.rate.toFloat()
                                    )
                                }
                            )
                        },
                        selectedTimeSpan = (interactiveAssetPrice ?: priceDetail).timeSpan
                    )
                }
            }
        }
    }

    private fun reduceTotalBalance(state: CoinviewModelState): CoinviewTotalBalanceState = state.run {
        when {
            // not supported for non custodial
            walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                CoinviewTotalBalanceState.NotSupported
            }

            assetInfo is DataResource.Loading -> {
                CoinviewTotalBalanceState.Loading
            }

            assetInfo is DataResource.Error -> {
                CoinviewTotalBalanceState.NotSupported
            }

            assetInfo is DataResource.Data && assetInfo.data is CoinviewAssetInformation.AccountsInfo -> {
                require(asset != null) { "asset not initialized" }

                with(assetInfo.data as CoinviewAssetInformation.AccountsInfo) {
                    require(totalBalance.totalCryptoBalance.containsKey(AssetFilter.All)) { "balance not initialized" }

                    CoinviewTotalBalanceState.Data(
                        assetName = asset.currency.name,
                        totalFiatBalance = totalBalance.totalFiatBalance.toStringWithSymbol(),
                        totalCryptoBalance = totalBalance.totalCryptoBalance[AssetFilter.All]!!.toStringWithSymbol()
                    )
                }
            }

            else -> {
                CoinviewTotalBalanceState.Loading
            }
        }
    }

    private fun reduceAccounts(state: CoinviewModelState): CoinviewAccountsState = state.run {
        when {
            assetInfo is DataResource.Loading -> {
                CoinviewAccountsState.Loading
            }

            assetInfo is DataResource.Data && assetInfo.data is CoinviewAssetInformation.AccountsInfo -> {
                require(asset != null) { "asset not initialized" }

                with(assetInfo.data as CoinviewAssetInformation.AccountsInfo) {
                    CoinviewAccountsState.Data(
                        style = when (accounts) {
                            is CoinviewAccounts.Universal,
                            is CoinviewAccounts.Custodial -> CoinviewAccountsStyle.Simple
                            is CoinviewAccounts.Defi -> CoinviewAccountsStyle.Boxed
                        },
                        header = when (accounts) {
                            is CoinviewAccounts.Universal,
                            is CoinviewAccounts.Custodial -> CoinviewAccountsHeaderState.ShowHeader(
                                SimpleValue.IntResValue(R.string.coinview_accounts_label)
                            )
                            is CoinviewAccounts.Defi -> CoinviewAccountsHeaderState.NoHeader
                        },
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
                                        is CoinviewAccount.Universal -> {
                                            Available(
                                                title = when (cvAccount.filter) {
                                                    AssetFilter.Trading -> labels.getDefaultCustodialWalletLabel()
                                                    AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
                                                    AssetFilter.NonCustodial -> account.label
                                                    else -> error(
                                                        "Filer ${cvAccount.filter} not supported for account label"
                                                    )
                                                },
                                                subtitle = when (cvAccount.filter) {
                                                    AssetFilter.Trading -> {
                                                        SimpleValue.IntResValue(R.string.coinview_c_available_desc)
                                                    }
                                                    AssetFilter.Interest -> {
                                                        SimpleValue.IntResValue(
                                                            R.string.coinview_interest_with_balance,
                                                            listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                                                        )
                                                    }
                                                    AssetFilter.NonCustodial -> {
                                                        if (account is MultiChainAccount) {
                                                            SimpleValue.IntResValue(
                                                                R.string.coinview_multi_nc_desc,
                                                                listOf(account.l1Network.networkName)
                                                            )
                                                        } else {
                                                            SimpleValue.IntResValue(R.string.coinview_nc_desc)
                                                        }
                                                    }
                                                    else -> error("${cvAccount.filter} Not a supported filter")
                                                },
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = LogoSource.Local(
                                                    when (cvAccount.filter) {
                                                        AssetFilter.Trading -> {
                                                            R.drawable.ic_custodial_account_indicator
                                                        }
                                                        AssetFilter.Interest -> {
                                                            R.drawable.ic_interest_account_indicator
                                                        }
                                                        AssetFilter.NonCustodial -> {
                                                            R.drawable.ic_non_custodial_account_indicator
                                                        }
                                                        else -> error("${cvAccount.filter} Not a supported filter")
                                                    }
                                                ),
                                                assetColor = asset.currency.colour
                                            )
                                        }
                                        is CoinviewAccount.Custodial.Trading -> {
                                            Available(
                                                title = labels.getDefaultCustodialWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(R.string.coinview_c_available_desc),
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = LogoSource.Local(R.drawable.ic_custodial_account_indicator),
                                                assetColor = asset.currency.colour
                                            )
                                        }
                                        is CoinviewAccount.Custodial.Interest -> {
                                            Available(
                                                title = labels.getDefaultInterestWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(
                                                    R.string.coinview_interest_with_balance,
                                                    listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                                                ),
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = LogoSource.Local(R.drawable.ic_interest_account_indicator),
                                                assetColor = asset.currency.colour
                                            )
                                        }
                                        is CoinviewAccount.Defi -> {
                                            Available(
                                                title = account.label,
                                                subtitle = SimpleValue.StringValue(account.currency.displayTicker),
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = LogoSource.Remote(account.currency.logo),
                                                assetColor = asset.currency.colour
                                            )
                                        }
                                    }
                                }

                                false -> {
                                    when (cvAccount) {
                                        is CoinviewAccount.Universal -> {
                                            Unavailable(
                                                title = when (cvAccount.filter) {
                                                    AssetFilter.Trading -> labels.getDefaultCustodialWalletLabel()
                                                    AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
                                                    AssetFilter.NonCustodial -> account.label
                                                    else -> error(
                                                        "Filer ${cvAccount.filter} not supported for account label"
                                                    )
                                                },
                                                subtitle = when (cvAccount.filter) {
                                                    AssetFilter.Trading -> {
                                                        SimpleValue.IntResValue(
                                                            R.string.coinview_c_unavailable_desc,
                                                            listOf(asset.currency.name)
                                                        )
                                                    }
                                                    AssetFilter.Interest -> {
                                                        SimpleValue.IntResValue(
                                                            R.string.coinview_interest_no_balance,
                                                            listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                                                        )
                                                    }
                                                    AssetFilter.NonCustodial -> {
                                                        SimpleValue.IntResValue(R.string.coinview_nc_desc)
                                                    }
                                                    else -> error("${cvAccount.filter} Not a supported filter")
                                                },
                                                logo = LogoSource.Local(
                                                    when (cvAccount.filter) {
                                                        AssetFilter.Trading -> {
                                                            R.drawable.ic_custodial_account_indicator
                                                        }
                                                        AssetFilter.Interest -> {
                                                            R.drawable.ic_interest_account_indicator
                                                        }
                                                        AssetFilter.NonCustodial -> {
                                                            R.drawable.ic_non_custodial_account_indicator
                                                        }
                                                        else -> error("${cvAccount.filter} Not a supported filter")
                                                    }
                                                )
                                            )
                                        }
                                        is CoinviewAccount.Custodial.Trading -> {
                                            Unavailable(
                                                title = labels.getDefaultCustodialWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(
                                                    R.string.coinview_c_unavailable_desc,
                                                    listOf(asset.currency.name)
                                                ),
                                                logo = LogoSource.Local(R.drawable.ic_custodial_account_indicator)
                                            )
                                        }
                                        is CoinviewAccount.Custodial.Interest -> {
                                            Unavailable(
                                                title = labels.getDefaultInterestWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(
                                                    R.string.coinview_interest_no_balance,
                                                    listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                                                ),
                                                logo = LogoSource.Local(R.drawable.ic_interest_account_indicator)
                                            )
                                        }
                                        is CoinviewAccount.Defi -> {
                                            Unavailable(
                                                title = account.currency.name,
                                                subtitle = SimpleValue.IntResValue(R.string.coinview_nc_desc),
                                                logo = LogoSource.Remote(account.currency.logo)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            else -> {
                CoinviewAccountsState.Loading
            }
        }
    }

    private fun reduceRecurringBuys(state: CoinviewModelState): CoinviewRecurringBuysState = state.run {
        when {
            // not supported for non custodial
            walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                CoinviewRecurringBuysState.NotSupported
            }

            recurringBuys is DataResource.Loading -> {
                CoinviewRecurringBuysState.Loading
            }

            recurringBuys is DataResource.Error -> {
                CoinviewRecurringBuysState.Error
            }

            recurringBuys is DataResource.Data -> {
                require(asset != null) { "asset not initialized" }

                with(recurringBuys.data) {
                    if (data.isEmpty()) {
                        if (isAvailableForTrading) {
                            CoinviewRecurringBuysState.Upsell
                        } else {
                            CoinviewRecurringBuysState.NotSupported
                        }
                    } else {
                        CoinviewRecurringBuysState.Data(
                            data.map { recurringBuy ->
                                RecurringBuyState(
                                    id = recurringBuy.id,
                                    description = SimpleValue.IntResValue(
                                        R.string.dashboard_recurring_buy_item_title_1,
                                        listOf(
                                            recurringBuy.amount.toStringWithSymbol(),
                                            recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                                        )
                                    ),

                                    status = if (recurringBuy.state ==
                                        com.blockchain.nabu.models.data.RecurringBuyState.ACTIVE
                                    ) {
                                        SimpleValue.IntResValue(
                                            R.string.dashboard_recurring_buy_item_label,
                                            listOf(recurringBuy.nextPaymentDate.toFormattedDateWithoutYear())
                                        )
                                    } else {
                                        SimpleValue.IntResValue(
                                            R.string.dashboard_recurring_buy_item_label_error
                                        )
                                    },

                                    assetColor = asset.currency.colour
                                )
                            }
                        )
                    }
                }
            }

            else -> {
                CoinviewRecurringBuysState.Loading
            }
        }
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
        when (intent) {
            is CoinviewIntents.LoadAllData -> {
                require(modelState.asset != null) { "asset not initialized" }
                onIntent(CoinviewIntents.LoadPriceData)
                onIntent(CoinviewIntents.LoadAccountsData)
                onIntent(CoinviewIntents.LoadRecurringBuysData)
            }

            CoinviewIntents.LoadPriceData -> {
                require(modelState.asset != null) { "asset not initialized" }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = (modelState.assetPriceHistory as? DataResource.Data)
                        ?.data?.priceDetail?.timeSpan ?: defaultTimeSpan
                )
            }

            CoinviewIntents.LoadAccountsData -> {
                require(modelState.asset != null) { "asset not initialized" }

                loadAccountsData(
                    asset = modelState.asset,
                )
            }

            CoinviewIntents.LoadRecurringBuysData -> {
                require(modelState.asset != null) { "asset not initialized" }

                loadRecurringBuysData(
                    asset = modelState.asset,
                )
            }

            is CoinviewIntents.UpdatePriceForChartSelection -> {
                require(modelState.assetPriceHistory is DataResource.Data) { "price data not initialized" }

                updatePriceForChartSelection(
                    entry = intent.entry,
                    assetPriceHistory = modelState.assetPriceHistory.data
                )
            }

            is CoinviewIntents.ResetPriceSelection -> {
                resetPriceSelection()
            }

            is CoinviewIntents.NewTimeSpanSelected -> {
                require(modelState.asset != null) { "asset not initialized" }

                updateState { it.copy(requestedTimeSpan = intent.timeSpan) }

                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = intent.timeSpan,
                )
            }

            CoinviewIntents.RecurringBuysUpsell -> {
                // todo
            }

            is CoinviewIntents.ShowRecurringBuyDetail -> {
                // todo
            }
        }
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
                asset = asset, timeSpan = requestedTimeSpan, fiatCurrency = fiatCurrency
            ).collectLatest { dataResource ->
                when (dataResource) {
                    DataResource.Loading -> {
                        updateState {
                            it.copy(
                                isChartDataLoading = true,
                                assetPriceHistory = if (it.assetPriceHistory is DataResource.Data) {
                                    it.assetPriceHistory
                                } else {
                                    dataResource
                                }
                            )
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isChartDataLoading = false,
                                assetPriceHistory = dataResource,
                            )
                        }
                    }

                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                it.copy(
                                    isChartDataLoading = false,
                                    assetPriceHistory = DataResource.Error(Exception("no historicRates"))
                                )
                            }
                        } else {
                            updateState {
                                it.copy(
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
        assetPriceHistory: CoinviewAssetPriceHistory,
    ) {
        val historicRates = assetPriceHistory.historicRates
        historicRates.firstOrNull { it.timestamp.toFloat() == entry.x }?.let { selectedHistoricalRate ->
            val firstForPeriod = historicRates.first()
            val difference = selectedHistoricalRate.rate - firstForPeriod.rate

            val percentChange = (difference / firstForPeriod.rate)

            val changeDifference = Money.fromMajor(fiatCurrency, difference.toBigDecimal())

            updateState {
                it.copy(
                    interactiveAssetPrice = CoinviewAssetPrice(
                        price = Money.fromMajor(
                            fiatCurrency, selectedHistoricalRate.rate.toBigDecimal()
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
        updateState { it.copy(interactiveAssetPrice = null) }
    }

    // //////////////////////
    // Accounts
    /**
     * Loads accounts and total balance
     */
    private fun loadAccountsData(asset: CryptoAsset) {
        viewModelScope.launch {
            loadAssetAccountsUseCase(asset = asset).collectLatest { dataResource ->

                updateState {
                    it.copy(
                        assetInfo = if (dataResource is DataResource.Loading && it.assetInfo is DataResource.Data) {
                            // if data is present already - don't show loading
                            it.assetInfo
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }

    // //////////////////////
    // Recurring buys
    private fun loadRecurringBuysData(asset: CryptoAsset) {
        viewModelScope.launch {
            loadAssetRecurringBuysUseCase(asset).collectLatest { dataResource ->
                updateState {
                    it.copy(
                        recurringBuys = if (dataResource is DataResource.Loading &&
                            it.recurringBuys is DataResource.Data
                        ) {
                            it.recurringBuys
                        } else {
                            dataResource
                        }
                    )
                }
            }
        }
    }
}
