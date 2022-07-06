package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.domain.models.BlockchainCardError
import com.blockchain.blockchaincard.domain.models.BlockchainCardStatus
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import exchange.ExchangeLinking
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AccountInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val blockchainCardRepository: BlockchainCardRepository,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeLinkingState: ExchangeLinking,
    private val localSettingsPrefs: LocalSettingsPrefs
) {

    fun getWalletInfo(): Single<AccountInformation> =
        settingsDataManager.getSettings().firstOrError().map {
            AccountInformation(
                walletId = it.guid,
                userCurrency = FiatCurrency.fromCurrencyCode(it.currency),
                isChartVibrationEnabled = localSettingsPrefs.isChartVibrationEnabled
            )
        }

    fun getAvailableFiatList(): Single<List<FiatCurrency>> =
        Single.just(exchangeRates.fiatAvailableForRates)

    fun updateSelectedCurrency(currency: FiatCurrency): Observable<Settings> =
        settingsDataManager.updateFiatUnit(currency)
            .doOnComplete {
                currencyPrefs.selectedFiatCurrency = currency
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
            .mapError { BlockchainCardError.GetCardsRequestFailed }
            .flatMap { cards ->
                val activeCards = cards.filter { it.status != BlockchainCardStatus.TERMINATED }
                if (activeCards.isNotEmpty()) {
                    // TODO(labreu): For now we only allow 1 card, but in the future we must pass the full list here
                    Outcome.Success(BlockchainCardOrderState.Ordered(activeCards.first()))
                } else {
                    blockchainCardRepository.getProducts()
                        .mapError { BlockchainCardError.GetProductsRequestFailed }
                        .map { products ->
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
}
