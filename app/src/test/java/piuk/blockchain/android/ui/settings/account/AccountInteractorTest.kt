package piuk.blockchain.android.ui.settings.account

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.settings.SettingsDataManager
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.fiatcurrencies.model.TradingCurrencies
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.testutils.CAD
import com.blockchain.testutils.EUR
import com.blockchain.testutils.GBP
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import exchange.ExchangeLinking
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountInteractorTest {

    private lateinit var interactor: AccountInteractor

    private val settingsDataManager: SettingsDataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val fiatCurrenciesService: FiatCurrenciesService = mock()
    private val exchangeLinkingState: ExchangeLinking = mock()
    private val localSettingsPrefs: LocalSettingsPrefs = mock()
    private val dustBalancesFF: FeatureFlag = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setup() {
        interactor = AccountInteractor(
            settingsDataManager = settingsDataManager,
            exchangeRates = exchangeRates,
            currencyPrefs = currencyPrefs,
            fiatCurrenciesService = fiatCurrenciesService,
            exchangeLinkingState = exchangeLinkingState,
            localSettingsPrefs = localSettingsPrefs,
            dustBalancesFF = dustBalancesFF
        )
    }

    @Test
    fun getWalletInfo() = runTest {
        val mockGuid = "12345"
        val currencyCode = "EUR"
        val chartVibrationEnabled = true
        val settingsMock: Settings = mock {
            on { guid }.thenReturn(mockGuid)
            on { currency }.thenReturn(currencyCode)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))
        whenever(localSettingsPrefs.isChartVibrationEnabled).thenReturn(chartVibrationEnabled)
        whenever(fiatCurrenciesService.getTradingCurrencies()).thenReturn(Outcome.Success(TRADING_CURRENCIES))

        val observable = interactor.getWalletInfo().test()
        observable.assertValue {
            it.walletId == mockGuid &&
                it.displayCurrency.displayTicker == currencyCode &&
                it.isChartVibrationEnabled == chartVibrationEnabled
        }

        verify(settingsDataManager).getSettings()
        verify(localSettingsPrefs).isChartVibrationEnabled
        verifyNoMoreInteractions(settingsDataManager)
        verifyNoMoreInteractions(localSettingsPrefs)
    }

    @Test
    fun getAvailableDisplayCurrencies() {
        val fiatList: List<FiatCurrency> = mock()
        whenever(exchangeRates.fiatAvailableForRates).thenReturn(fiatList)

        val list = interactor.getAvailableDisplayCurrencies().test()
        list.assertValue {
            it == fiatList
        }
        verify(exchangeRates).fiatAvailableForRates
        verifyNoMoreInteractions(exchangeRates)
    }

    @Test
    fun getAvailableTradingCurrencies() = runTest {
        whenever(fiatCurrenciesService.getTradingCurrencies()).thenReturn(Outcome.Success(TRADING_CURRENCIES))

        val list = interactor.getAvailableTradingCurrencies().test()
        list.assertValue {
            it == TRADING_CURRENCIES.allAvailable
        }
        verify(fiatCurrenciesService).getTradingCurrencies()
    }

    @Test
    fun updateSelectedDisplayCurrency() {
        val settingsMock: Settings = mock()
        val fiatCurrency = EUR

        whenever(settingsDataManager.updateFiatUnit(fiatCurrency)).thenReturn(Observable.just(settingsMock))

        val observable = interactor.updateSelectedDisplayCurrency(fiatCurrency).test()
        observable.assertValue {
            it == settingsMock
        }

        verify(settingsDataManager).updateFiatUnit(fiatCurrency)
        verify(currencyPrefs).selectedFiatCurrency = fiatCurrency

        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions((settingsDataManager))
    }

    @Test
    fun updateSelectedTradingCurrency() = runTest {
        whenever(fiatCurrenciesService.setSelectedTradingCurrency(EUR)).thenReturn(Outcome.Success(Unit))

        interactor.updateSelectedTradingCurrency(EUR).test()

        verify(fiatCurrenciesService).setSelectedTradingCurrency(EUR)
    }

    companion object {
        private val TRADING_CURRENCIES = TradingCurrencies(
            selected = USD,
            allRecommended = listOf(USD, GBP),
            allAvailable = listOf(USD, GBP, EUR, CAD)
        )
    }
}
