package piuk.blockchain.android.ui.coinview.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.data.DataResource
import com.blockchain.preferences.CurrencyPrefs
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
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetInformation
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Available
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountState.Unavailable
import piuk.blockchain.android.ui.coinview.presentation.CoinviewAccountsState.Data.CoinviewAccountsHeaderState

class CoinviewViewModel(
    walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val getAssetPriceUseCase: GetAssetPriceUseCase,
    private val loadAssetAccountsUseCase: LoadAssetAccountsUseCase
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
                    asset = asset,
                    isPriceDataLoading = true,
                )
            }
        } ?: error("asset ${args.networkTicker} not found")
    }

    override fun reduce(state: CoinviewModelState): CoinviewViewState = state.run {
        CoinviewViewState(
            assetName = asset?.currency?.name ?: "",

            assetPrice = when {
                isPriceDataLoading && assetPriceHistory == null -> {
                    // show loading when data is loading and no data is previously available
                    CoinviewPriceState.Loading
                }

                isPriceDataError -> {
                    CoinviewPriceState.Error
                }

                assetPriceHistory != null -> {
                    // priceFormattedWithFiatSymbol, priceChangeFormattedWithFiatSymbol, percentChange
                    // will contain values from interactiveAssetPrice to correspond with user interaction

                    // intervalName will be empty if user is interacting with the chart

                    require(asset != null) { "asset not initialized" }

                    CoinviewPriceState.Data(
                        assetName = asset.currency.name,
                        assetLogo = asset.currency.logo,
                        fiatSymbol = fiatCurrency.symbol,
                        price = (interactiveAssetPrice ?: assetPriceHistory.priceDetail)
                            .price.toStringWithSymbol(),
                        priceChange = (interactiveAssetPrice ?: assetPriceHistory.priceDetail)
                            .changeDifference.toStringWithSymbol(),
                        percentChange = (interactiveAssetPrice ?: assetPriceHistory.priceDetail).percentChange,
                        intervalName = if (interactiveAssetPrice != null) R.string.empty else
                            when ((assetPriceHistory.priceDetail).timeSpan) {
                                HistoricalTimeSpan.DAY -> R.string.coinview_price_day
                                HistoricalTimeSpan.WEEK -> R.string.coinview_price_week
                                HistoricalTimeSpan.MONTH -> R.string.coinview_price_month
                                HistoricalTimeSpan.YEAR -> R.string.coinview_price_year
                                HistoricalTimeSpan.ALL_TIME -> R.string.coinview_price_all
                            },
                        chartData = when {
                            isPriceDataLoading &&
                                requestedTimeSpan != null &&
                                assetPriceHistory.priceDetail.timeSpan != requestedTimeSpan -> {
                                // show chart loading when data is loading and a new timespan is selected
                                CoinviewPriceState.Data.CoinviewChart.Loading
                            }
                            else -> CoinviewPriceState.Data.CoinviewChart.Data(
                                assetPriceHistory.historicRates.map { point ->
                                    ChartEntry(
                                        point.timestamp.toFloat(),
                                        point.rate.toFloat()
                                    )
                                }
                            )
                        },
                        selectedTimeSpan = (interactiveAssetPrice ?: assetPriceHistory.priceDetail).timeSpan
                    )
                }

                else -> {
                    CoinviewPriceState.Loading
                }
            },

            totalBalance = when {
                // not supported for non custodial
                walletMode == WalletMode.NON_CUSTODIAL_ONLY -> {
                    CoinviewTotalBalanceState.NotSupported
                }

                isTotalBalanceLoading && totalBalance == null -> {
                    CoinviewTotalBalanceState.Loading
                }

                isTotalBalanceError -> {
                    CoinviewTotalBalanceState.NotSupported
                }

                totalBalance != null -> {
                    require(asset != null) { "asset not initialized" }
                    require(totalBalance.totalCryptoBalance.containsKey(AssetFilter.All)) { "balance not initialized" }

                    CoinviewTotalBalanceState.Data(
                        assetName = asset.currency.name,
                        totalFiatBalance = totalBalance.totalFiatBalance.toStringWithSymbol(),
                        totalCryptoBalance = totalBalance.totalCryptoBalance[AssetFilter.All]!!.toStringWithSymbol()
                    )
                }

                else -> {
                    CoinviewTotalBalanceState.Loading
                }
            },

            accounts = when {
                isAccountsLoading && accounts == null -> {
                    CoinviewAccountsState.Loading
                }

                accounts != null -> {
                    require(asset != null) { "asset not initialized" }

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

                            when (cvAccount.isAvailable) {
                                true -> {
                                    when (cvAccount) {
                                        is CoinviewAccount.Universal -> TODO()
                                        is CoinviewAccount.Custodial.Trading -> {
                                            Available(
                                                title = labels.getDefaultCustodialWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(R.string.coinview_c_available_desc),
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = account.currency.logo
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
                                                logo = account.currency.logo
                                            )
                                        }
                                        is CoinviewAccount.Defi -> {
                                            Available(
                                                title = account.label,
                                                subtitle = SimpleValue.StringValue(account.currency.displayTicker),
                                                cryptoBalance = cvAccount.cryptoBalance.toStringWithSymbol(),
                                                fiatBalance = cvAccount.fiatBalance.toStringWithSymbol(),
                                                logo = account.currency.logo
                                            )
                                        }
                                    }
                                }

                                false -> {
                                    when (cvAccount) {
                                        is CoinviewAccount.Universal -> TODO()
                                        is CoinviewAccount.Custodial.Trading -> {
                                            Unavailable(
                                                title = labels.getDefaultCustodialWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(
                                                    R.string.coinview_c_unavailable_desc,
                                                    listOf(asset.currency.name)
                                                ),
                                            )
                                        }
                                        is CoinviewAccount.Custodial.Interest -> {
                                            Unavailable(
                                                title = labels.getDefaultInterestWalletLabel(),
                                                subtitle = SimpleValue.IntResValue(
                                                    R.string.coinview_interest_no_balance,
                                                    listOf(DecimalFormat("0.#").format(cvAccount.interestRate))
                                                ),
                                            )
                                        }
                                        is CoinviewAccount.Defi -> {
                                            Unavailable(
                                                title = account.currency.name,
                                                subtitle = SimpleValue.IntResValue(R.string.coinview_nc_desc),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                else -> {
                    CoinviewAccountsState.Loading
                }
            }
        )
    }

    override suspend fun handleIntent(modelState: CoinviewModelState, intent: CoinviewIntents) {
        when (intent) {
            is CoinviewIntents.LoadData -> {
                require(modelState.asset != null) { "asset not initialized" }
                loadPriceData(
                    asset = modelState.asset,
                    requestedTimeSpan = modelState.assetPriceHistory?.priceDetail?.timeSpan ?: defaultTimeSpan
                )
                loadAccountsData(
                    asset = modelState.asset,
                )
            }

            is CoinviewIntents.UpdatePriceForChartSelection -> {
                updatePriceForChartSelection(intent.entry, modelState.assetPriceHistory?.historicRates!!)
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
                            it.copy(isPriceDataLoading = true)
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isPriceDataLoading = false,
                                isPriceDataError = true,
                            )
                        }
                    }

                    is DataResource.Data -> {
                        if (dataResource.data.historicRates.isEmpty()) {
                            updateState {
                                it.copy(
                                    isPriceDataLoading = false,
                                    isPriceDataError = true
                                )
                            }
                        } else {
                            updateState {
                                it.copy(
                                    isPriceDataLoading = false,
                                    isPriceDataError = false,
                                    assetPriceHistory = dataResource.data,
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
        historicRates: List<HistoricalRate>,
    ) {
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
                        timeSpan = it.assetPriceHistory!!.priceDetail.timeSpan,
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
     * Loads accounts and todo
     */
    private fun loadAccountsData(asset: CryptoAsset) {
        viewModelScope.launch {
            loadAssetAccountsUseCase(asset = asset).collectLatest { dataResource ->
                when (dataResource) {
                    DataResource.Loading -> {
                        updateState {
                            it.copy(
                                isTotalBalanceLoading = true,
                                isAccountsLoading = true
                            )
                        }
                    }

                    is DataResource.Error -> {
                        updateState {
                            it.copy(
                                isTotalBalanceLoading = false,
                                isTotalBalanceError = true,

                                isAccountsLoading = false,
                                isAccountsError = true
                            )
                        }
                    }

                    is DataResource.Data -> {
                        updateState {
                            it.copy(
                                isTotalBalanceLoading = false,
                                isTotalBalanceError = false,

                                isAccountsLoading = false,
                                isAccountsError = false
                            )
                        }

                        when (dataResource.data) {
                            is CoinviewAssetInformation.AccountsInfo -> {
                                (dataResource.data as CoinviewAssetInformation.AccountsInfo).let { data ->
                                    extractTotalBalance(data)
                                    extractAccounts(data)
                                }
                            }
                            is CoinviewAssetInformation.NonTradeable -> {
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractTotalBalance(accountsInfo: CoinviewAssetInformation.AccountsInfo) {
        updateState {
            it.copy(totalBalance = accountsInfo.totalBalance)
        }
    }

    private fun extractAccounts(accountsInfo: CoinviewAssetInformation.AccountsInfo) {
        updateState {
            it.copy(accounts = accountsInfo.accounts)
        }
    }
}
