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
    ModelConfigArgs.NoArgs>(PricesModelState(fiatCurrency = currencyPrefs.selectedFiatCurrency)) {

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
                loadAssetsAndPrices().asFlow().onEach { priceInfo ->
                    updateState {
                        it.updateAsset(priceInfo)
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
                compareByDescending<PricesItem> { it.priceWithDelta?.marketCap }
                    .thenBy { it.isTradingAccount }
                    .thenBy { it.assetInfo.name }
            ).map {
                it.toPriceItemViewModel(state.fiatCurrency)
            }
        )
    }

    private fun loadAssetsAndPrices(): Observable<AssetPriceInfo> {
        updateState { it.copy(isLoadingData = true) }

        return walletModeService.walletMode.map { walletMode ->
            coincore.availableCryptoAssets().filter {
                when (walletMode) {
                    WalletMode.NON_CUSTODIAL_ONLY -> it.isNonCustodial
                    WalletMode.CUSTODIAL_ONLY -> it.isCustodial
                    WalletMode.UNIVERSAL -> true
                }
            }
        }.asObservable().doOnNext { availableAssets ->
            updateState {
                modelState.updateAllAssets(availableAssets)
            }
        }.flatMapIterable { assets ->
            assets.map { fetchAssetPrice(it) }
        }.flatMapSingle { it }
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo): Single<AssetPriceInfo> {
        return exchangeRatesDataManager.getPricesWith24hDelta(assetInfo).firstOrError()
            .flatMap { prices24HrWithDelta ->
                custodialWalletManager.isCurrencyAvailableForTrading(assetInfo).map {
                    AssetPriceInfo(
                        price = prices24HrWithDelta,
                        isTradable = it,
                        assetInfo = assetInfo
                    )
                }
            }.onErrorReturn {
                AssetPriceInfo(
                    assetInfo = assetInfo,
                    isTradable = false
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

private fun PricesModelState.updateAsset(assetPriceInfo: AssetPriceInfo): PricesModelState =
    copy(
        isLoadingData = false,
        isError = false,
        data = this.data.minus { it.assetInfo.networkTicker == assetPriceInfo.assetInfo.networkTicker }.plus(
            PricesItem(
                assetInfo = assetPriceInfo.assetInfo,
                hasError = assetPriceInfo.price == null,
                isTradingAccount = assetPriceInfo.isTradable,
                priceWithDelta = assetPriceInfo.price
            )
        )
    )

private data class AssetPriceInfo(
    val price: Prices24HrWithDelta? = null,
    val assetInfo: AssetInfo,
    val isTradable: Boolean,
)
