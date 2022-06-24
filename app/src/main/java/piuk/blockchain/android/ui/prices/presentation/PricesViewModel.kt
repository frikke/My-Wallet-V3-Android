package piuk.blockchain.android.ui.prices.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.launch
import piuk.blockchain.android.domain.usecases.GetAvailableCryptoAssetsUseCase
import piuk.blockchain.android.ui.prices.PricesItem
import piuk.blockchain.android.ui.prices.PricesModelState
import timber.log.Timber

class PricesViewModel(
    private val getAvailableCryptoAssetsUseCase: GetAvailableCryptoAssetsUseCase,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
) : MviViewModel<PricesIntents,
    PricesViewState,
    PricesModelState,
    PricesNavigationEvent,
    ModelConfigArgs.NoArgs>(PricesModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override suspend fun handleIntent(modelState: PricesModelState, intent: PricesIntents) {
        when (intent) {
            is PricesIntents.LoadAssetsAvailable -> loadAssetsAvailable()

            is PricesIntents.LoadPrice -> fetchAssetPrice(intent.cryptoCurrency)

            is PricesIntents.FilterData -> filterData(intent.filter)

            is PricesIntents.PricesItemClicked -> {
                handlePriceItemClicked(
                    cryptoCurrency = intent.cryptoCurrency
                )
            }
        }.exhaustive
    }

    override fun reduce(state: PricesModelState): PricesViewState {
        return PricesViewState(
            isLoading = state.isLoadingData,
            isError = state.isError,
            data = state.data.values.sortedWith(
                compareByDescending<PricesItem> { it.priceWithDelta?.marketCap }
                    .thenBy { it.isTradingAccount }
                    .thenBy { it.assetInfo.name }
            ).run {
                if (state.filterBy.isNotEmpty()) {
                    filter {
                        it.assetInfo.displayTicker.contains(state.filterBy, ignoreCase = true) ||
                            it.assetInfo.name.contains(state.filterBy, ignoreCase = true)
                    }
                } else this
            }
        )
    }

    private fun loadAssetsAvailable() {
        updateState { it.copy(isLoadingData = true) }
        getAvailableCryptoAssetsUseCase(Unit)
            .subscribeBy(
                onSuccess = { assets ->
                    assets.forEach { assetInfo ->
                        fetchAssetPrice(assetInfo)
                    }
                },
                onError = { error ->
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("Error loading available assets $error")
                }
            )
    }

    private fun fetchAssetPrice(assetInfo: AssetInfo) {
        exchangeRatesDataManager.getPricesWith24hDelta(assetInfo)
            // If prices are coming in too fast, be sure not to miss any
            .toFlowable(BackpressureStrategy.BUFFER)
            .flatMap { prices24HrWithDelta ->
                custodialWalletManager.isCurrencyAvailableForTrading(assetInfo)
                    .toObservable().toFlowable(BackpressureStrategy.BUFFER)
                    .map { prices24HrWithDelta to it }
            }.subscribeBy(
                onNext = { (prices24HrWithDelta, isTradingAccount) ->
                    updateState {
                        it.copy(
                            isLoadingData = false,
                            isError = false,
                            data = addPriceToAsset(it.data, assetInfo, prices24HrWithDelta, isTradingAccount)
                        )
                    }
                },
                onError = { error ->
                    updateState { it.copy(isLoadingData = false, isError = true) }
                    Timber.e("error loading price $error")
                }
            )
    }

    private fun addPriceToAsset(
        data: MutableMap<AssetInfo, PricesItem>,
        assetInfo: AssetInfo,
        priceWithDelta: Prices24HrWithDelta,
        isTradingAccount: Boolean,
    ): MutableMap<AssetInfo, PricesItem> {
        data[assetInfo] = (PricesItem(assetInfo, currencyPrefs.selectedFiatCurrency, priceWithDelta, isTradingAccount))
        return data
    }

    private fun filterData(filter: String) = updateState { it.copy(filterBy = filter) }

    private fun handlePriceItemClicked(cryptoCurrency: AssetInfo) {
        viewModelScope.launch {
            navigate(PricesNavigationEvent.CoinView(cryptoCurrency))
        }
    }
}
