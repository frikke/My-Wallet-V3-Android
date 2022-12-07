package com.blockchain.prices.prices

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.PricesPrefs
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.store.mapListData
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency.BTC
import info.blockchain.balance.CryptoCurrency.ETHER
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PricesViewModel(
    private val walletModeService: WalletModeService,
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore,
    private val pricesPrefs: PricesPrefs,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val simpleBuyService: SimpleBuyService,
    private val watchlistService: WatchlistService
) : MviViewModel<PricesIntents,
    PricesViewState,
    PricesModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs>(
    PricesModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: PricesModelState): PricesViewState {
        return PricesViewState(
            availableFilters = state.filters,
            selectedFilter = state.filterBy,
            data = state.data
                .filter { assetPriceInfo ->
                    state.filterTerm.isEmpty() ||
                        assetPriceInfo.assetInfo.displayTicker.contains(state.filterTerm, ignoreCase = true) ||
                        assetPriceInfo.assetInfo.name.contains(state.filterTerm, ignoreCase = true)
                }
                .filter { assetPriceInfo ->
                    when (state.filterBy) {
                        PricesFilter.All -> {
                            true
                        }
                        PricesFilter.Tradable -> {
                            state.tradableCurrencies.map {
                                it.contains(assetPriceInfo.assetInfo.networkTicker)
                            }.dataOrElse(false)
                        }
                        PricesFilter.Favorites -> {
                            state.watchlist.map {
                                it.contains(assetPriceInfo.assetInfo.networkTicker)
                            }.dataOrElse(false)
                        }
                    }
                }
                .map {
                    it.sortedWith(
                        compareByDescending<AssetPriceInfo> { assetPriceInfo ->
                            state.tradableCurrencies.map {
                                it.contains(assetPriceInfo.assetInfo.networkTicker)
                            }.dataOrElse(false)
                        }.thenByDescending {
                            it.price.map { it.marketCap }.dataOrElse(null)
                        }.thenBy {
                            it.assetInfo.name
                        }
                    )
                }.mapList {
                    it.toPriceItemViewModel()
                }
        )
    }

    private fun AssetPriceInfo.toPriceItemViewModel(): PriceItemViewState {
        return PriceItemViewState(
            name = assetInfo.name,
            ticker = assetInfo.networkTicker,
            logo = assetInfo.logo,
            delta = price.map { ValueChange.fromValue(it.delta24h) },
            currentPrice = price.map {
                it.currentRate.price.format(currencyPrefs.selectedFiatCurrency)
            }
        )
    }

    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadAssetsAvailable -> {
                loadFilters()
                loadAvailableAssets()
            }

            is PricesIntents.FilterSearch -> {
                updateState {
                    it.copy(filterTerm = intent.term)
                }
            }

            is PricesIntents.PricesItemClicked -> {
                handlePriceItemClicked(
                    cryptoCurrency = intent.cryptoCurrency
                )
            }
            is PricesIntents.Filter -> {
                updateState {
                    it.copy(filterBy = intent.filter)
                }
            }
        }
    }

    private fun loadAvailableAssets() {
        viewModelScope.launch {
            val tradableCurrenciesFlow = simpleBuyService.getSupportedBuySellCryptoCurrencies()
                .mapListData { it.source.networkTicker }

            val watchlistFlow = watchlistService.getWatchlist()
                .mapData { (it + defaultWatchlist).distinct() }.mapListData { it.networkTicker }

            val pricesFlow = loadAssetsAndPrices()

            combine(
                tradableCurrenciesFlow,
                watchlistFlow,
                pricesFlow
            ) { tradableCurrencies, watchlist, prices ->
                updateState {
                    modelState.copy(
                        tradableCurrencies = it.tradableCurrencies.updateDataWith(tradableCurrencies),
                        watchlist = it.watchlist.updateDataWith(watchlist),
                        data = it.data.updateDataWith(prices)
                    )
                }
            }.collect()
        }
    }

    private fun loadFilters() {
        updateState {
            it.copy(
                filters = listOf(
                    PricesFilter.All, PricesFilter.Favorites, PricesFilter.Tradable
                )
            )
        }
        // we should define which filters are for each wallet mode
        // atm they are the same - uncomment once they are different
        //        viewModelScope.launch {
        //            walletModeService.walletMode
        //                .onEach {
        //                    updateState {
        //                        it.copy(
        //                            filters = listOf(
        //                                PricesFilter.All, PricesFilter.Favorites, PricesFilter.Tradable
        //                            )
        //                        )
        //                    }
        //                }
        //                .collect()
        //        }
        //
        //        collectLatest {
        //            updateState { state ->
        //                if (it != WalletMode.UNIVERSAL) {
        //                    state.copy(
        //                        filters = listOf(
        //                            PricesFilter.All, PricesFilter.Tradable
        //                        ),
        //                        filterBy = initialSelectedFilter(pricesPrefs.latestPricesMode, it)
        //                    )
        //                } else {
        //                    state.copy(
        //                        filters = emptyList()
        //                    )
        //                }
        //            }
        //        }
    }

    private fun initialSelectedFilter(latestPricesMode: String?, walletMode: WalletMode): PricesFilter {
        if (latestPricesMode == null) {
            return when (walletMode) {
                WalletMode.CUSTODIAL_ONLY -> PricesFilter.Tradable
                WalletMode.NON_CUSTODIAL_ONLY -> PricesFilter.All
                else -> throw IllegalArgumentException("Price filtering is not supported for $walletMode")
            }
        }
        return try {
            PricesFilter.valueOf(latestPricesMode)
        } catch (e: IllegalArgumentException) {
            PricesFilter.All
        }
    }

    private fun loadAssetsAndPrices(): Flow<DataResource<List<AssetPriceInfo>>> {
        return coincore.availableCryptoAssetsFlow().flatMapData {
            val assetPriceInfoList = it.map { assetInfo ->
                exchangeRatesDataManager
                    .getPricesWith24hDelta(
                        assetInfo,
                        FreshnessStrategy.Cached(forceRefresh = false)
                    )
                    .map { priceDataResource ->
                        AssetPriceInfo(
                            price = priceDataResource,
                            assetInfo = assetInfo
                        )
                    }
            }

            combine(assetPriceInfoList) {
                it.toList()
            }.map {
                //                if (it.any { it.price is DataResource.Loading }) {
                //                    DataResource.Loading
                //                } else {
                DataResource.Data(it)
                //                }

            }
        }
        //        val supportedBuyCurrencies = custodialWalletManager.getSupportedBuySellCryptoCurrencies()
        //        return supportedBuyCurrencies.doOnSuccess { tradableCurrencies ->
        //            updateState {
        //                modelState.copy(tradableCurrencies = tradableCurrencies.map { it.source.networkTicker })
        //            }
        //        }.flatMap {
        //            coincore.availableCryptoAssets().doOnSuccess {
        //                updateState {
        //                    modelState.copy(
        //                        isLoadingData = true,
        //                        queryBy = "",
        //                        data = emptyList()
        //                    )
        //                }
        //            }.flatMap { assets ->
        //                Single.concat(
        //                    assets.map { fetchAssetPrice(it) }
        //                ).toList()
        //            }
        //        }
    }

    //    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<AssetPriceInfo> {
    //
    //        return exchangeRatesDataManager.getPricesWith24hDeltaLegacy(assetInfo).firstOrError()
    //            .map { prices24HrWithDelta ->
    //                AssetPriceInfo(
    //                    price = prices24HrWithDelta,
    //                    assetInfo = assetInfo
    //                )
    //            }.subscribeOn(Schedulers.io()).onErrorReturn {
    //                AssetPriceInfo(
    //                    assetInfo = assetInfo
    //                )
    //            }
    //    }

    private fun handlePriceItemClicked(cryptoCurrency: AssetInfo) {
        //        navigate(PricesNavigationEvent.CoinView(cryptoCurrency))
    }

    companion object {
        val defaultWatchlist = listOf(BTC, ETHER)
    }
}

fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()