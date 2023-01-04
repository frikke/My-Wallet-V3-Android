package piuk.blockchain.android.ui.settings.account

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.utils.rxCompletableOutcome
import com.blockchain.utils.rxSingleOutcome
import exchange.ExchangeLinking
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher

class AccountInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeLinkingState: ExchangeLinking,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val dustBalancesFF: FeatureFlag
) {

    fun getWalletInfo(): Single<AccountInformation> =
        Singles.zip(
            settingsDataManager.getSettings().firstOrError(),
            rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
                fiatCurrenciesService.getTradingCurrencies()
            }
        ) { settings, tradingCurrencies ->
            AccountInformation(
                walletId = settings.guid,
                displayCurrency = FiatCurrency.fromCurrencyCode(settings.currency),
                tradingCurrency = tradingCurrencies.selected,
                isChartVibrationEnabled = localSettingsPrefs.isChartVibrationEnabled
            )
        }

    fun getAvailableDisplayCurrencies(): Single<List<FiatCurrency>> =
        Single.just(exchangeRates.fiatAvailableForRates)

    fun getAvailableTradingCurrencies(): Single<List<FiatCurrency>> =
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            fiatCurrenciesService.getTradingCurrencies().map {
                it.allAvailable
            }
        }

    fun updateSelectedDisplayCurrency(currency: FiatCurrency): Observable<Settings> =
        settingsDataManager.updateFiatUnit(currency)
            .doOnComplete {
                currencyPrefs.selectedFiatCurrency = currency
            }

    fun updateSelectedTradingCurrency(currency: FiatCurrency): Completable =
        rxCompletableOutcome(Schedulers.io().asCoroutineDispatcher()) {
            fiatCurrenciesService.setSelectedTradingCurrency(currency)
        }

    fun getExchangeState(): Single<ExchangeLinkingState> =
        exchangeLinkingState.state.firstOrError().map {
            if (it.isLinked) {
                ExchangeLinkingState.LINKED
            } else {
                ExchangeLinkingState.NOT_LINKED
            }
        }

    fun toggleChartVibration(chartVibrationEnabled: Boolean): Single<Boolean> =
        Single.fromCallable {
            localSettingsPrefs.isChartVibrationEnabled = !chartVibrationEnabled
            return@fromCallable !chartVibrationEnabled
        }
    fun loadFeatureFlags(): Single<FeatureFlagSet> =
        dustBalancesFF.enabled.map { dustBalancesEnabled ->
            FeatureFlagSet(
                dustBalancesEnabled
            )
        }
}
