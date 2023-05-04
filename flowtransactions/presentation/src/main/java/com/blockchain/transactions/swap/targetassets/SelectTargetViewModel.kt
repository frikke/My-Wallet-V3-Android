package com.blockchain.transactions.swap.targetassets

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.SwapService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SelectTargetViewModel(
    private val sourceTicker: String,
    private val swapService: SwapService,
    private val pricesService: PricesService,
    private val currencyPrefs: CurrencyPrefs,
    private val walletModeService: WalletModeService
) : MviViewModel<SelectTargetIntent,
    SelectTargetViewState,
    SelectTargetModelState,
    TargetAssetNavigationEvent,
    ModelConfigArgs.NoArgs>(
    SelectTargetModelState()
) {

    init {
        viewModelScope.launch {
            val walletMode = walletModeService.walletMode.firstOrNull()
            updateState {
                it.copy(
                    walletMode = walletMode,
                    selectedAssetsModeFilter = walletMode
                )
            }
        }
    }

    private var pricesJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectTargetModelState) = state.run {
        SelectTargetViewState(
            showModeFilter = walletMode == WalletMode.NON_CUSTODIAL,
            selectedModeFilter = selectedAssetsModeFilter,
            prices = prices
                .filter { asset ->
                    state.filterTerm.isEmpty() ||
                        asset.assetInfo.displayTicker.contains(state.filterTerm, ignoreCase = true) ||
                        asset.assetInfo.name.contains(state.filterTerm, ignoreCase = true)
                }
                .map { list ->
                    /**
                     * sorted by: watchlist - asset index - marketcap
                     */
                    list.sortedWith(
                        compareByDescending<AssetPriceInfo> { asset ->
                            asset.isInWatchlist
                        }.thenByDescending { asset ->
                            asset.assetInfo.index
                        }.thenByDescending { asset ->
                            asset.price.map { price -> price.marketCap }.dataOrElse(null)
                        }.thenBy {
                            it.assetInfo.name
                        }
                    )
                }
                .mapList {
                    it.toViewState(withNetwork = selectedAssetsModeFilter == WalletMode.NON_CUSTODIAL)
                }
        )
    }

    override suspend fun handleIntent(modelState: SelectTargetModelState, intent: SelectTargetIntent) {
        when (intent) {
            is SelectTargetIntent.LoadData -> {
                loadPrices()
            }

            is SelectTargetIntent.FilterSearch -> {
                updateState {
                    it.copy(
                        filterTerm = intent.term
                    )
                }
            }

            is SelectTargetIntent.ModeFilterSelected -> {
                updateState {
                    it.copy(
                        selectedAssetsModeFilter = intent.selected
                    )
                }
                loadPrices()
            }

            is SelectTargetIntent.AssetSelected -> {
                check(modelState.selectedAssetsModeFilter != null)
                viewModelScope.launch {
                    swapService
                        .accountsWithBalanceOfMode(
                            sourceTicker,
                            intent.ticker,
                            modelState.selectedAssetsModeFilter
                        )
                        .filterIsInstance<DataResource.Data<List<CryptoAccountWithBalance>>>()
                        .firstOrNull()
                        ?.data?.let {
                            if (it.count() == 1) {
                                navigate(TargetAssetNavigationEvent.ConfirmSelection(account = it.first().account))
                            } else {
                                navigate(TargetAssetNavigationEvent.SelectAccount(ofTicker = intent.ticker))
                            }
                        }
                }
            }
        }
    }

    private fun loadPrices() {
        pricesJob?.cancel()
        pricesJob = modelState.selectedAssetsModeFilter?.let { selectedAssetsModeFilter ->
            viewModelScope.launch {
                swapService.targetTickersForMode(sourceTicker, selectedAssetsModeFilter).let { tickers ->
                    pricesService.assets(tickers)
                        .collectLatest { pricesData ->
                            updateState {
                                it.copy(
                                    prices = it.prices.updateDataWith(pricesData)
                                )
                            }
                        }
                }
            }
        }
    }

    private fun AssetPriceInfo.toViewState(
        withNetwork: Boolean = true
    ): BalanceChange {
        return BalanceChange(
            name = assetInfo.name,
            ticker = assetInfo.networkTicker,
            network = assetInfo.takeIf { it.isLayer2Token }?.coinNetwork?.shortName?.takeIf { withNetwork },
            logo = assetInfo.logo,
            delta = price.map { ValueChange.fromValue(it.delta24h) },
            currentPrice = price.map {
                it.currentRate.price.format(currencyPrefs.selectedFiatCurrency)
            },
            showRisingFastTag = false
        )
    }
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
