package piuk.blockchain.android.ui.settings.v2.account

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.bccard.BcCardDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import thepit.PitLinking

class AccountInteractor internal constructor(
    private val settingsDataManager: SettingsDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val bcCardDataManager: BcCardDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val exchangeLinkingState: PitLinking,
) {

    fun getWalletInfo(): Single<AccountInformation> =
        settingsDataManager.getSettings().firstOrError().map {
            AccountInformation(
                walletId = it.guid,
                userCurrency = FiatCurrency.fromCurrencyCode(it.currency)
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

    fun getDebitCardState(): Single<DebitCardState> =
        bcCardDataManager.getProducts().flatMap {
            if (it.isNotEmpty())
                Single.just(DebitCardState.ORDERED)
            else
                Single.just(DebitCardState.NOT_ORDERED)
        }

}
