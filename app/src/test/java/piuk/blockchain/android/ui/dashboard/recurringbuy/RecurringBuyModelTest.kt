package piuk.blockchain.android.ui.dashboard.recurringbuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyError
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyIntent
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyInteractor
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModel
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModelState
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyViewState
import retrofit2.HttpException

class RecurringBuyModelTest {

    private lateinit var subject: RecurringBuyModel

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: RecurringBuyInteractor = mock()

    private val defaultState = RecurringBuyModelState()

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        subject = RecurringBuyModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor
        )
    }

    @Test
    fun `loading recurring buy call success should fire correct intent`() {
        val rbId = "rbid"
        val rbMock: RecurringBuy = mock()
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))

        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.recurringBuy == rbMock &&
                it.viewState == RecurringBuyViewState.ShowRecurringBuy
        }
    }

    @Test
    fun `loading recurring buy call fails should fire correct error intent`() {
        val rbId = "rbid"
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.error(Exception()))

        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.error == RecurringBuyError.LoadFailed
        }
    }

    @Test
    fun `deleting recurring buy call success should fire correct intent`() {
        val rbId = "rbid"
        val rbMock = RecurringBuy(
            id = rbId,
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = mock(),
            nextPaymentDate = mock(),
            paymentMethodType = mock(),
            paymentMethodId = "",
            amount = mock(),
            asset = mock(),
            createDate = mock()
        )
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))
        whenever(interactor.deleteRecurringBuy(rbId)).thenReturn(Completable.complete())

        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))
        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.DeleteRecurringBuy)

        testState.assertValueAt(0) {
            it.recurringBuy == rbMock
        }.assertValueAt(1) {
            it.recurringBuy?.state == RecurringBuyState.INACTIVE
        }
    }

    @Test
    fun `deleting recurring buy call fails should fire correct error intent`() {
        val rbId = "rbid"
        val rbMock: RecurringBuy = mock {
            on { id }.thenReturn(rbId)
        }
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))
        whenever(interactor.deleteRecurringBuy(rbId)).thenReturn(Completable.error(Exception()))

        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))
        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.DeleteRecurringBuy)

        testState.assertValueAt(0) {
            it.recurringBuy == rbMock
        }.assertValueAt(1) {
            it.error == RecurringBuyError.RecurringBuyDelete
        }
    }

    @Test
    fun `loading recurring buy payment details call success should fire correct intent`() {
        val rbId = "rbid"
        val rbMock = RecurringBuy(
            id = rbId,
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = mock(),
            nextPaymentDate = mock(),
            paymentMethodType = mock(),
            paymentMethodId = "",
            amount = mock {
                on { currencyCode }.thenReturn("USD")
            },
            asset = mock(),
            createDate = mock()
        )

        val pdMock: RecurringBuyPaymentDetails = mock()
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))
        whenever(
            interactor.loadPaymentDetails(
                rbMock.paymentMethodType, rbMock.paymentMethodId.orEmpty(), rbMock.amount.currencyCode
            )
        ).thenReturn(Single.just(pdMock))

        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))
        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.GetPaymentDetails)

        testState.assertValueAt(0) {
            it.recurringBuy == rbMock
        }.assertValueAt(1) {
            it.recurringBuy?.paymentDetails == pdMock
        }
    }

    @Test
    fun `loading recurring buy payment details call fails with nabu exception should fire correct error intent`() {
        val rbId = "rbid"
        val rbMock = RecurringBuy(
            id = rbId,
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = mock(),
            nextPaymentDate = mock(),
            paymentMethodType = mock(),
            paymentMethodId = "",
            amount = mock {
                on { currencyCode }.thenReturn("USD")
            },
            asset = mock(),
            createDate = mock()
        )

        val errorMessage = "error Message"
        val exception = HttpException(
            mock {
                on { code() }.thenReturn(500)
                on { message() }.thenReturn(errorMessage)
            }
        )
        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))
        whenever(
            interactor.loadPaymentDetails(
                rbMock.paymentMethodType, rbMock.paymentMethodId.orEmpty(), rbMock.amount.currencyCode
            )
        ).thenReturn(Single.error(exception))

        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))
        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.GetPaymentDetails)

        testState.assertValueAt(0) {
            it.recurringBuy == rbMock
        }.assertValueAt(1) {
            it.error is RecurringBuyError.HttpError &&
                (it.error as RecurringBuyError.HttpError).errorMessage == "500"
        }
    }

    @Test
    fun `loading recurring buy payment details call fails with other exception should fire correct error intent`() {
        val rbId = "rbid"
        val rbMock = RecurringBuy(
            id = rbId,
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = mock(),
            nextPaymentDate = mock(),
            paymentMethodType = mock(),
            paymentMethodId = "",
            amount = mock {
                on { currencyCode }.thenReturn("USD")
            },
            asset = mock(),
            createDate = mock()
        )

        whenever(interactor.getRecurringBuyById(rbId)).thenReturn(Single.just(rbMock))
        whenever(
            interactor.loadPaymentDetails(
                rbMock.paymentMethodType, rbMock.paymentMethodId.orEmpty(), rbMock.amount.currencyCode
            )
        ).thenReturn(Single.error(Exception()))

        subject.process(RecurringBuyIntent.LoadRecurringBuy(rbId))
        val testState = subject.state.test()
        subject.process(RecurringBuyIntent.GetPaymentDetails)

        testState.assertValueAt(0) {
            it.recurringBuy == rbMock
        }.assertValueAt(1) {
            it.error == RecurringBuyError.UnknownError
        }
    }
}
