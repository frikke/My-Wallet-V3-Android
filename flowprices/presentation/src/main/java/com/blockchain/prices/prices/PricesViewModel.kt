package com.blockchain.prices.prices

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.doOnData
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.onErrorReturn
import com.blockchain.data.updateDataWith
import com.blockchain.nabu.Feature
import com.blockchain.nabu.api.getuser.domain.UserFeaturePermissionService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.prices.domain.PricesService
import com.blockchain.utils.CurrentTimeProvider
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PricesViewModel(
    private val walletModeService: WalletModeService,
    private val currencyPrefs: CurrencyPrefs,
    private val userFeaturePermissionService: UserFeaturePermissionService,
    private val pricesService: PricesService,
    private val dispatcher: CoroutineDispatcher
) : MviViewModel<
    PricesIntents,
    PricesViewState,
    PricesModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    PricesModelState()
) {

    private var pricesJob: Job? = null
    private var topMoversCountJob: Job? = null
    private var mostPopularJob: Job? = null
    private var risingFastJob: Job? = null
    private var filtersJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    init {
        loadWalletMode()
    }

    override fun PricesModelState.reduce() = PricesViewState(
        walletMode = walletMode,
        availableFilters = filters,
        selectedFilter = filterBy,
        data = data
            .filter { asset ->
                filterTerm.isEmpty() ||
                    asset.assetInfo.displayTicker.contains(filterTerm, ignoreCase = true) ||
                    asset.assetInfo.name.contains(filterTerm, ignoreCase = true)
            }
            .filter { asset ->
                when (filterBy) {
                    PricesFilter.All -> {
                        true
                    }
                    PricesFilter.Tradable -> {
                        asset.isTradable
                    }
                    PricesFilter.Favorites -> {
                        asset.isInWatchlist
                    }
                }
            }
            .map { list ->
                /**
                 * sorted by: watchlist - asset index - is tradable - marketcap
                 */
                list.sortedWith(
                    compareByDescending<AssetPriceInfo> { asset ->
                        asset.isInWatchlist
                    }.thenByDescending { asset ->
                        asset.assetInfo.index
                    }.thenByDescending { asset ->
                        asset.isTradable
                    }.thenByDescending { asset ->
                        asset.price.map { price -> price.marketCap }.dataOrElse(null)
                    }.thenBy {
                        it.assetInfo.name
                    }
                )
            }
            .mapList {
                it.toPriceItemViewState(
                    risingFastPercent = risingFastPercent,
                    withNetwork = walletMode != WalletMode.CUSTODIAL ||
                        filterBy != PricesFilter.Tradable
                )
            }
            .map {
                it.groupBy {
                    if (it.data.ticker in mostPopularTickers) {
                        PricesOutputGroup.MostPopular
                    } else PricesOutputGroup.Others
                }
            },
        topMovers = data.map { list ->
            fun AssetPriceInfo.tradableFilter() = when (loadStrategy) {
                PricesLoadStrategy.TradableOnly -> isTradable
                PricesLoadStrategy.All -> true
            }

            list.filter { it.price is DataResource.Data && it.tradableFilter() }
                .sortedWith(
                    compareByDescending { asset ->
                        asset.price.map {
                            (it.delta24h.takeIf { !it.isNaN() } ?: 0.0).absoluteValue
                        }.dataOrElse(0.0)
                    }
                )
                .take(topMoversCount)
                .map {
                    it.toPriceItemViewState(risingFastPercent = risingFastPercent)
                }
        }
    )

    private fun AssetPriceInfo.toPriceItemViewState(
        risingFastPercent: Double,
        withNetwork: Boolean = true
    ): PriceItemViewState {
        return PriceItemViewState(
            asset = assetInfo,
            data = BalanceChange(
                name = assetInfo.name,
                ticker = assetInfo.displayTicker,
                network = assetInfo.takeIf { it.isLayer2Token }?.coinNetwork?.shortName?.takeIf { withNetwork },
                logo = assetInfo.logo,
                nativeAssetLogo = null,
                delta = price.map { ValueChange.fromValue(it.delta24h) },
                currentPrice = price.map {
                    it.currentRate.price.format(currencyPrefs.selectedFiatCurrency)
                },
                showRisingFastTag = price.map { it.delta24h >= risingFastPercent }.dataOrElse(false)
            )
        )
    }

    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadData -> {
                updateState {
                    copy(
                        loadStrategy = intent.strategy,
                        filterBy = when (intent.strategy) {
                            PricesLoadStrategy.All -> PricesFilter.All
                            PricesLoadStrategy.TradableOnly -> PricesFilter.Tradable
                        }
                    )
                }

                loadFilters()
                loadData(intent.strategy)
                loadTopMoversCount()
                loadMostPopularTickers()
                loadRisingFastPercentThreshold()
            }

            is PricesIntents.FilterSearch -> {
                updateState {
                    copy(filterTerm = intent.term)
                }
            }

            is PricesIntents.Filter -> {
                updateState {
                    copy(filterBy = intent.filter)
                }
            }

            PricesIntents.Refresh -> {
                updateState {
                    copy(lastFreshDataTime = CurrentTimeProvider.currentTimeMillis())
                }
                loadData(strategy = modelState.loadStrategy)
            }
        }
    }

    private fun loadWalletMode() {
        viewModelScope.launch {
            walletModeService.walletMode.collectLatest { walletMode ->
                updateState {
                    copy(walletMode = walletMode)
                }
            }
        }
    }

    private fun loadData(strategy: PricesLoadStrategy) {
        pricesJob?.cancel()
        pricesJob = viewModelScope.launch(dispatcher) {
            val assetsFlow = when (strategy) {
                is PricesLoadStrategy.All -> pricesService.allAssets()
                is PricesLoadStrategy.TradableOnly -> pricesService.tradableAssets()
            }
            assetsFlow.catch {
                emit(DataResource.Error(it as Exception))
            }.collectLatest { prices ->
                updateState {
                    modelState.copy(
                        data = data.updateDataWith(prices)
                    )
                }
            }
        }
    }

    private fun loadFilters() {
        filtersJob?.cancel()
        filtersJob = viewModelScope.launch(dispatcher) {
            userFeaturePermissionService.isEligibleFor(Feature.CustodialAccounts)
                .onErrorReturn { true }.doOnData { canTrade ->
                    updateState {
                        copy(
                            filters = listOfNotNull(
                                PricesFilter.All,
                                PricesFilter.Favorites,
                                if (canTrade) PricesFilter.Tradable else null
                            )
                        )
                    }
                }.collect()
        }
    }

    private fun loadTopMoversCount() {
        topMoversCountJob?.cancel()
        topMoversCountJob = viewModelScope.launch(dispatcher) {
            pricesService.topMoversCount().catch {
                emit(0)
            }
                .collectLatest { count ->
                    updateState {
                        copy(
                            topMoversCount = count
                        )
                    }
                }
        }
    }

    private fun loadMostPopularTickers() {
        mostPopularJob?.cancel()
        mostPopularJob = viewModelScope.launch(dispatcher) {
            pricesService.mostPopularTickers()
                .collectLatest { mostPopularTickers ->
                    updateState {
                        copy(
                            mostPopularTickers = mostPopularTickers
                        )
                    }
                }
        }
    }

    private fun loadRisingFastPercentThreshold() {
        risingFastJob?.cancel()
        risingFastJob = viewModelScope.launch(dispatcher) {
            pricesService.risingFastPercentThreshold()
                .collectLatest { risingFastPercent ->
                    updateState {
                        copy(
                            risingFastPercent = risingFastPercent
                        )
                    }
                }
        }
    }
}

private fun Money?.format(cryptoCurrency: Currency) =
    this?.toStringWithSymbol()
        ?: Money.zero(cryptoCurrency).toStringWithSymbol()
