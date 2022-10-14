package piuk.blockchain.android.ui.settings.account

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import exchange.ExchangeLinking
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.rxCompletableOutcome
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

class AccountInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val blockchainCardRepository: BlockchainCardRepository,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeLinkingState: ExchangeLinking,
    private val localSettingsPrefs: LocalSettingsPrefs,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val blockchainCardFF: FeatureFlag,
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

    suspend fun getDebitCardState(): Outcome<BlockchainCardError, BlockchainCardOrderState> =
        blockchainCardRepository.getCards()
            .mapError { it }
            .flatMap { cards ->
                blockchainCardRepository.getProducts()
                    .mapError { it }
                    .map { products ->
                        if (cards.isNotEmpty()) {
                            val defaultCardId = blockchainCardRepository.getDefaultCard()
                            cards.find { it.id == defaultCardId }?.let { defaultCard ->
                                BlockchainCardOrderState.Ordered(
                                    cardProducts = products,
                                    cards = cards.reversed(),
                                    defaultCard = defaultCard
                                )
                            } ?: BlockchainCardOrderState.Ordered(
                                cardProducts = products,
                                cards = cards.reversed()
                            )
                        } else {
                            if (products.isNotEmpty())
                                BlockchainCardOrderState.Eligible(products)
                            else
                                BlockchainCardOrderState.NotEligible
                        }
                    }
            }

    fun toggleChartVibration(chartVibrationEnabled: Boolean): Single<Boolean> =
        Single.fromCallable {
            localSettingsPrefs.isChartVibrationEnabled = !chartVibrationEnabled
            return@fromCallable !chartVibrationEnabled
        }
    fun loadFeatureFlags(): Single<FeatureFlagSet> =
        Single.zip(
            blockchainCardFF.enabled,
            dustBalancesFF.enabled
        ) { blockchainCardEnabled, dustBalancesEnabled ->
            FeatureFlagSet(
                blockchainCardEnabled,
                dustBalancesEnabled
            )
        }
}
