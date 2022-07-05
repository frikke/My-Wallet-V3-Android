package piuk.blockchain.android.ui.settings.account

import com.blockchain.android.testutils.rxInit
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.testutils.EUR
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.settings.v2.account.AccountError
import piuk.blockchain.android.ui.settings.v2.account.AccountInformation
import piuk.blockchain.android.ui.settings.v2.account.AccountIntent
import piuk.blockchain.android.ui.settings.v2.account.AccountInteractor
import piuk.blockchain.android.ui.settings.v2.account.AccountModel
import piuk.blockchain.android.ui.settings.v2.account.AccountState
import piuk.blockchain.android.ui.settings.v2.account.ViewToLaunch

class AccountModelTest {

    private lateinit var model: AccountModel
    private var defaultState = AccountState(
        accountInformation = AccountInformation("1234", EUR, false)
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
        whenever(interactor.getAvailableFiatList()).thenReturn(Single.just(fiatList))

        val testState = model.state.test()
        model.process(AccountIntent.LoadFiatList)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.viewToLaunch is ViewToLaunch.CurrencySelection &&
                    (it.viewToLaunch as ViewToLaunch.CurrencySelection).currencyList == fiatList &&
                    (it.viewToLaunch as ViewToLaunch.CurrencySelection).selectedCurrency == EUR
            }
    }

    @Test
    fun loadFiatList_error() {
        whenever(interactor.getAvailableFiatList()).thenReturn(Single.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.LoadFiatList)

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.errorState == AccountError.FIAT_LIST_FAIL
            }
    }

    @Test
    fun updateFiatCurrency_success() {
        val newCurrency = USD
        whenever(interactor.updateSelectedCurrency(newCurrency)).thenReturn(Observable.just(mock()))

        val testState = model.state.test()
        model.process(AccountIntent.UpdateFiatCurrency(newCurrency))

        testState
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it.accountInformation?.userCurrency == newCurrency
            }
    }

    @Test
    fun updateFiatCurrency_error() {
        val newCurrency = USD
        whenever(interactor.updateSelectedCurrency(newCurrency)).thenReturn(Observable.error(Exception()))

        val testState = model.state.test()
        model.process(AccountIntent.UpdateFiatCurrency(newCurrency))

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
