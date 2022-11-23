package piuk.blockchain.android.ui.prices.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.PricesPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.prices.PricesItem
import piuk.blockchain.android.ui.prices.PricesModelState

class PricesViewModel(
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val pricesPrefs: PricesPrefs,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
) : MviViewModel<PricesIntents,
    PricesViewState,
    PricesModelState,
    PricesNavigationEvent,
    ModelConfigArgs.NoArgs>(
    PricesModelState(tradableCurrencies = emptyList())
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadAssetsAvailable -> {
                updateState {
                    modelState.copy(fiatCurrency = intent.fiatCurrency)
                }
                loadAvailableAssets()
            }
            is PricesIntents.Search -> searchData(intent.query)

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
            try {
                loadAssetsAndPrices().toObservable().asFlow().onEach { prices ->
                    updateState {
                        modelState.updateAssets(prices)
                    }
                }.collect()
            } catch (e: Exception) {
                updateState {
                    modelState.copy(isError = true, isLoadingData = false, data = emptyList(), fiatCurrency = null)
                }
            }
            walletModeService.walletMode.collectLatest {
                updateState { state ->
                    if (it != WalletMode.UNIVERSAL) {
                        state.copy(
                            filters = listOf(
                                PricesFilter.All, PricesFilter.Tradable
                            ),
                            filterBy = initialSelectedFilter(pricesPrefs.latestPricesMode, it)
                        )
                    } else {
                        state.copy(
                            filters = emptyList()
                        )
                    }
                }
            }
        }
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

    override fun reduce(state: PricesModelState): PricesViewState {
        return PricesViewState(
            isLoading = state.isLoadingData,
            isError = state.isError,
            availableFilters = state.filters,
            selectedFilter = state.filterBy,
            data = state.data.filter {
                state.filterBy == PricesFilter.All || it.isTradingAccount == true
            }.filter {
                state.queryBy.isEmpty() ||
                    it.assetInfo.displayTicker.contains(state.queryBy, ignoreCase = true) ||
                    it.assetInfo.name.contains(state.queryBy, ignoreCase = true)
            }.sortedWith(
                compareByDescending<PricesItem> { it.isTradingAccount }
                    .thenByDescending { it.priceWithDelta?.marketCap }
                    .thenBy { it.assetInfo.name }
            ).map {
                it.toPriceItemViewModel()
            }
        )
    }

    private fun loadAssetsAndPrices(): Single<List<AssetPriceInfo>> {
        val supportedBuyCurrencies = custodialWalletManager.getSupportedBuySellCryptoCurrencies()
        return supportedBuyCurrencies.doOnSuccess { tradableCurrencies ->
            updateState {
                modelState.copy(tradableCurrencies = tradableCurrencies.map { it.source.networkTicker })
            }
        }.flatMap {
            coincore.availableCryptoAssets().doOnSuccess {
                updateState {
                    modelState.copy(
                        isLoadingData = true,
                        queryBy = "",
                        data = emptyList()
                    )
                }
            }.flatMap { assets ->
                Single.concat(
                    assets.map { fetchAssetPrice(it) }
                ).toList()
            }
        }
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<AssetPriceInfo> {

        return exchangeRatesDataManager.getPricesWith24hDeltaLegacy(assetInfo).firstOrError()
            .map { prices24HrWithDelta ->
                AssetPriceInfo(
                    price = prices24HrWithDelta,
                    assetInfo = assetInfo
                )
            }.subscribeOn(Schedulers.io()).onErrorReturn {
                AssetPriceInfo(
                    assetInfo = assetInfo
                )
            }
    }

    private fun searchData(query: String) = updateState { it.copy(queryBy = query) }

    private fun handlePriceItemClicked(cryptoCurrency: AssetInfo) {
        navigate(PricesNavigationEvent.CoinView(cryptoCurrency))
    }
}

private fun PricesItem.toPriceItemViewModel(): PriceItemViewState {
    return PriceItemViewState(
        hasError = this.hasError,
        assetInfo = this.assetInfo,
        delta = priceWithDelta?.delta24h,
        currentPrice = (
            priceWithDelta?.currentRate?.price?.let {
                it.format(currency)
            } ?: "--"
            )
    )
}

private fun PricesModelState.updateAssets(assetPriceInfo: List<AssetPriceInfo>): PricesModelState {
    require(this.fiatCurrency != null)
    return copy(
        isLoadingData = false,
        isError = assetPriceInfo.isEmpty(),
        data = assetPriceInfo.map { priceInfo ->
            PricesItem(
                isTradingAccount = tradableCurrencies.contains(priceInfo.assetInfo.networkTicker),
                assetInfo = priceInfo.assetInfo,
                hasError = priceInfo.price == null,
                currency = this.fiatCurrency,
                priceWithDelta = priceInfo.price
            )
        }
    )
}

private data class AssetPriceInfo(
    val price: Prices24HrWithDelta? = null,
    val assetInfo: AssetInfo,
)

enum class PricesFilter {
    All, Tradable
}
