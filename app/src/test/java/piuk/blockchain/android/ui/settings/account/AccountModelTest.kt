package piuk.blockchain.android.ui.settings.account

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.testutils.EUR
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountModelTest {

    private lateinit var model: AccountModel
    private var defaultState = AccountState(
        accountInformation = AccountInformation("1234", EUR, EUR, false)
    )

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: AccountInteractor = mock()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = AccountModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeCache = mock(),
            interactor = interactor
        )
    }

    @Test
    fun getAccountInfo_success() {
        val walletInfoMock: AccountInformation = mock()
        whenever(interactor.getWalletInfo()).thenReturn(Single.just(walletInfoMock))

        val testState = model.state.test()
        model.process(AccountIntent.LoadAccountInformation)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it == AccountState(
                    accountInformation = walletInfoMock
                )
            }
    }

    @Test
    fun getAccountInfo_error() {
        whenever(interactor.getWalletInfo()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.LoadAccountInformation)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it == defaultState.copy(
                    errorState = AccountError.ACCOUNT_INFO_FAIL
                )
            }
    }

    @Test
    fun loadFiatList_success() {
        val fiatList: List<FiatCurrency> = mock()
        whenever(interactor.getAvailableDisplayCurrencies()).thenReturn(Single.just(fiatList))

        val testState = model.state.test()
        model.process(AccountIntent.LoadDisplayCurrencies)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.DisplayCurrencySelection &&
                    (it.viewToLaunch as ViewToLaunch.DisplayCurrencySelection).currencyList == fiatList &&
                    (it.viewToLaunch as ViewToLaunch.DisplayCurrencySelection).selectedCurrency == EUR
            }
    }

    @Test
    fun loadFiatList_error() {
        whenever(interactor.getAvailableDisplayCurrencies()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.LoadDisplayCurrencies)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.errorState == AccountError.FIAT_LIST_FAIL
            }
    }

    @Test
    fun updateDisplayFiatCurrency_success() {
        val newCurrency = USD
        whenever(interactor.updateSelectedDisplayCurrency(newCurrency)).thenReturn(Observable.just(mock()))

        val testState = model.state.test()
        model.process(AccountIntent.UpdateSelectedDisplayCurrency(newCurrency))

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.accountInformation?.displayCurrency == newCurrency
            }
    }

    @Test
    fun updateDisplayFiatCurrency_error() {
        val newCurrency = USD
        whenever(interactor.updateSelectedDisplayCurrency(newCurrency)).thenReturn(Observable.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.UpdateSelectedDisplayCurrency(newCurrency))

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.errorState == AccountError.ACCOUNT_FIAT_UPDATE_FAIL
            }
    }

    @Test
    fun updateTradingFiatCurrency_success() {
        val newCurrency = USD
        whenever(interactor.updateSelectedTradingCurrency(newCurrency)).thenReturn(Completable.complete())

        val testState = model.state.test()
        model.process(AccountIntent.UpdateSelectedTradingCurrency(newCurrency))

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.accountInformation?.tradingCurrency == newCurrency
            }
    }

    @Test
    fun updateTradingFiatCurrency_error() {
        val newCurrency = USD
        whenever(interactor.updateSelectedTradingCurrency(newCurrency)).thenReturn(Completable.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.UpdateSelectedTradingCurrency(newCurrency))

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.errorState == AccountError.ACCOUNT_FIAT_UPDATE_FAIL
            }
    }

    @Test
    fun toggleChartVibration() {
        val expectedReturn = !defaultState.accountInformation?.isChartVibrationEnabled!!
        whenever(interactor.toggleChartVibration(defaultState.accountInformation?.isChartVibrationEnabled!!))
            .thenReturn(Single.just(expectedReturn))

        val testState = model.state.test()
        model.process(AccountIntent.ToggleChartVibration)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.accountInformation?.isChartVibrationEnabled == expectedReturn
            }
    }
}
