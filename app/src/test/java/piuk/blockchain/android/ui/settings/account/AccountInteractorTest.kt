package piuk.blockchain.android.ui.settings.account

import com.blockchain.blockchaincard.data.BcCardDataRepository
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.EUR
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.account.AccountInteractor
import piuk.blockchain.android.ui.settings.v2.account.ExchangeLinkingState
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import thepit.PitLinking
import thepit.PitLinkingState

class AccountInteractorTest {

    private lateinit var interactor: AccountInteractor

    private val settingsDataManager: SettingsDataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val exchangeLinkingState: PitLinking = mock()
    private val bcCardDataRepository: BcCardDataRepository = mock()

    @Before
    fun setup() {
        interactor = AccountInteractor(
            settingsDataManager = settingsDataManager,
            exchangeRates = exchangeRates,
            currencyPrefs = currencyPrefs,
            exchangeLinkingState = exchangeLinkingState,
            bcCardDataRepository = bcCardDataRepository
        )
    }

    @Test
    fun getWalletInfo() {
        val mockGuid = "12345"
        val currencyCode = "EUR"
        val settingsMock: Settings = mock {
            on { guid }.thenReturn(mockGuid)
            on { currency }.thenReturn(currencyCode)
        }
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settingsMock))

        val observable = interactor.getWalletInfo().test()
        observable.assertValue {
            it.walletId == mockGuid &&
                it.userCurrency.displayTicker == currencyCode
        }

        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun getAvailableFiatList() {
        val fiatList: List<FiatCurrency> = mock()
        whenever(exchangeRates.fiatAvailableForRates).thenReturn(fiatList)

        val list = interactor.getAvailableFiatList().test()
        list.assertValue {
            it == fiatList
        }
        verify(exchangeRates).fiatAvailableForRates
        verifyNoMoreInteractions(exchangeRates)
    }

    @Test
    fun updateSelectedCurrency() {
        val settingsMock: Settings = mock()
        val fiatCurrency = EUR

        whenever(settingsDataManager.updateFiatUnit(fiatCurrency)).thenReturn(Observable.just(settingsMock))

        val observable = interactor.updateSelectedCurrency(fiatCurrency).test()
        observable.assertValue {
            it == settingsMock
        }

        verify(settingsDataManager).updateFiatUnit(fiatCurrency)
        verify(currencyPrefs).selectedFiatCurrency = fiatCurrency

        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions((settingsDataManager))
    }

    @Test
    fun getExchangeState_linked() {
        val exchangeState = PitLinkingState(isLinked = true)
        whenever(exchangeLinkingState.state).thenReturn(Observable.just(exchangeState))

        val result = interactor.getExchangeState().test()
        result.assertValue {
            it == ExchangeLinkingState.LINKED
        }

        verify(exchangeLinkingState).state
        verifyNoMoreInteractions(exchangeLinkingState)
    }

    @Test
    fun getExchangeState_notLinked() {
        val exchangeState = PitLinkingState(isLinked = false)
        whenever(exchangeLinkingState.state).thenReturn(Observable.just(exchangeState))

        val result = interactor.getExchangeState().test()
        result.assertValue {
            it == ExchangeLinkingState.NOT_LINKED
        }

        verify(exchangeLinkingState).state
        verifyNoMoreInteractions(exchangeLinkingState)
    }
}
