package piuk.blockchain.android.ui.prices.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.extensions.minus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.isCustodial
import info.blockchain.balance.isNonCustodial
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.prices.PricesItem
import piuk.blockchain.android.ui.prices.PricesModelState

class PricesViewModel(
    private val walletModeService: WalletModeService,
    private val coincore: Coincore,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    currencyPrefs: CurrencyPrefs,
) : MviViewModel<PricesIntents,
    PricesViewState,
    PricesModelState,
    PricesNavigationEvent,
    ModelConfigArgs.NoArgs>(
    PricesModelState(fiatCurrency = currencyPrefs.selectedFiatCurrency, tradableCurrencies = emptyList())
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}
    private var job: Job? = null
    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadAssetsAvailable -> {
                job?.cancel()
                loadAvailableAssets()
            }
            is PricesIntents.FilterData -> filterData(intent.filter)

            is PricesIntents.PricesItemClicked -> {
                handlePriceItemClicked(
                    cryptoCurrency = intent.cryptoCurrency
                )
            }
        }
    }

    private fun loadAvailableAssets() {
        job = viewModelScope.launch {
            try {
                loadAssetsAndPrices().asFlow().onEach { prices ->
                    updateState {
                        modelState.updateAssets(prices)
                    }
                }.collect()
            } catch (e: Exception) {
                updateState {
                    modelState.copy(isError = true, isLoadingData = false, data = emptyList())
                }
            }
        }
    }

    override fun reduce(state: PricesModelState): PricesViewState {
        return PricesViewState(
            isLoading = state.isLoadingData,
            isError = state.isError,
            data = state.data.filter {
                state.filterBy.isEmpty() ||
                    it.assetInfo.displayTicker.contains(state.filterBy, ignoreCase = true) ||
                    it.assetInfo.name.contains(state.filterBy, ignoreCase = true)
            }.sortedWith(
                compareByDescending<PricesItem> { it.isTradingAccount }
                    .thenByDescending { it.priceWithDelta?.marketCap }
                    .thenBy { it.assetInfo.name }
            ).map {
                it.toPriceItemViewModel(state.fiatCurrency)
            }
        )
    }

    private fun loadAssetsAndPrices(): Observable<List<AssetPriceInfo>> {
        val supportedBuyCurrencies = custodialWalletManager.getSupportedBuySellCryptoCurrencies()
        return supportedBuyCurrencies.doOnSuccess { tradableCurrencies ->
            updateState {
                modelState.copy(tradableCurrencies = tradableCurrencies.map { it.source.networkTicker })
            }
        }.flatMapObservable {
            walletModeService.walletMode.map { walletMode ->
                coincore.availableCryptoAssets().filter {
                    when (walletMode) {
                        WalletMode.NON_CUSTODIAL_ONLY -> it.isNonCustodial
                        WalletMode.CUSTODIAL_ONLY -> it.isCustodial
                        WalletMode.UNIVERSAL -> true
                    }
                }
            }.asObservable().doOnNext {
                updateState {
                    modelState.copy(
                        isLoadingData = true,
                        data = emptyList()
                    )
                }
            }.flatMapSingle { assets ->
                Single.concat(
                    assets.map { fetchAssetPrice(it) }
                ).toList()
            }
        }
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<AssetPriceInfo> {

        return exchangeRatesDataManager.getPricesWith24hDelta(assetInfo).firstOrError()
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

    private fun filterData(filter: String) = updateState { it.copy(filterBy = filter) }

    private fun handlePriceItemClicked(cryptoCurrency: AssetInfo) {
        navigate(PricesNavigationEvent.CoinView(cryptoCurrency))
    }
}

private fun PricesItem.toPriceItemViewModel(fiatCurrency: Currency): PriceItemViewState {
    return PriceItemViewState(
        hasError = this.hasError,
        assetInfo = this.assetInfo,
        delta = priceWithDelta?.delta24h,
        currentPrice = (
            priceWithDelta?.currentRate?.price?.let {
                it.format(fiatCurrency)
            } ?: "--"
            )
    )
}

private fun PricesModelState.updateAllAssets(assets: List<AssetInfo>): PricesModelState =
    copy(
        isLoadingData = false,
        isError = false,
        data = assets.map {
            PricesItem(
                assetInfo = it,
                hasError = false
            )
        }
    )

private fun PricesModelState.updateAssets(assetPriceInfo: List<AssetPriceInfo>): PricesModelState =
    copy(
        isLoadingData = false,
        isError = assetPriceInfo.isEmpty(),
        data = assetPriceInfo.map { priceInfo ->
            PricesItem(
                isTradingAccount = tradableCurrencies.contains(priceInfo.assetInfo.networkTicker),
                assetInfo = priceInfo.assetInfo,
                hasError = priceInfo.price == null,
                priceWithDelta = priceInfo.price
            )
        }
    )

private data class AssetPriceInfo(
    val price: Prices24HrWithDelta? = null,
    val assetInfo: AssetInfo,
)
