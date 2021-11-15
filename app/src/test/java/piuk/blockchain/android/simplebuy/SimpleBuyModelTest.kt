package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.nabu.models.responses.simplebuy.EverypayPaymentAttrs
import com.blockchain.nabu.models.responses.simplebuy.PaymentAttributes
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Date
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.cards.EverypayAuthOptions
import piuk.blockchain.android.cards.partners.EverypayCardActivator
import piuk.blockchain.android.domain.usecases.GetEligibilityAndNextPaymentDateUseCase
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

@Ignore("Ignoring because CI fails on this, re-enabling ASAP")
class SimpleBuyModelTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val defaultState = SimpleBuyState(
        selectedCryptoAsset = CryptoCurrency.BTC,
        amount = FiatValue.fromMinor("USD", 1000),
        fiatCurrency = "USD",
        selectedPaymentMethod = SelectedPaymentMethod(
            id = "123-321",
            paymentMethodType = PaymentMethodType.PAYMENT_CARD,
            isEligible = true
        ),
        id = "123"
    )

    private val interactor: SimpleBuyInteractor = mock()
    private val getEligibilityAndNextPaymentDateUseCase: GetEligibilityAndNextPaymentDateUseCase = mock()

    private val prefs: SimpleBuyPrefs = mock {
        on { simpleBuyState() }.thenReturn(Gson().toJson(defaultState))
    }

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val ratingPrefs: RatingPrefs = mock {
        on { hasSeenRatingDialog }.thenReturn(true)
        on { preRatingActionCompletedTimes }.thenReturn(0)
    }

    private val serializer: SimpleBuyPrefsSerializer = mock()

    private val model = SimpleBuyModel(
        prefs = prefs,
        initialState = defaultState,
        uiScheduler = Schedulers.io(),
        interactor = interactor,
        cardActivators = listOf(
            mock()
        ),
        ratingPrefs = ratingPrefs,
        environmentConfig = environmentConfig,
        crashLogger = mock(),
        _activityIndicator = mock(),
        serializer = serializer,
        isFirstTimeBuyerUseCase = mock(),
        getEligibilityAndNextPaymentDateUseCase = mock(),
        bankPartnerCallbackProvider = mock(),
        userIdentity = mock {
            on { isVerifiedFor(Feature.TierLevel(Tier.GOLD)) }.thenReturn(Single.just(true))
        },
        currencyPrefs = mock()
    )

    @Test
    fun `cancel order should make the order to cancel if interactor doesnt return an error`() {
        whenever(interactor.cancelOrder(any()))
            .thenReturn(Completable.complete())

        val expectedState = SimpleBuyState(orderState = OrderState.CANCELED)

        model.process(SimpleBuyIntent.CancelOrder)
        model.state
            .test()
            .awaitCount(2)
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it == expectedState }
    }

    @Ignore("Fails on CI, works locally. Re-enable ASAP")
    @Test
    fun `confirm order should make the order to confirm if interactor doesnt return an error`() {
        val date = Date()
        whenever(
            interactor.createOrder(
                cryptoAsset = anyOrNull(),
                amount = anyOrNull(),
                paymentMethodId = anyOrNull(),
                paymentMethod = any(),
                isPending = any(),
                recurringBuyFrequency = anyOrNull()
            )
        ).thenReturn(
            Single.just(
                SimpleBuyIntent.OrderCreated(
                    BuySellOrder(
                        id = "testId",
                        expires = date,
                        state = OrderState.AWAITING_FUNDS,
                        crypto = CryptoValue.zero(CryptoCurrency.BTC),
                        orderValue = CryptoValue.zero(CryptoCurrency.BTC),
                        paymentMethodId = "213",
                        updated = Date(),
                        paymentMethodType = PaymentMethodType.FUNDS,
                        fiat = FiatValue.zero("USD"),
                        pair = "USD-BTC",
                        type = OrderType.BUY,
                        depositPaymentId = ""
                    )
                )
            )
        )

        val expectedState = defaultState.copy(
            orderState = OrderState.AWAITING_FUNDS,
            id = "testId",
            orderValue = CryptoValue.zero(CryptoCurrency.BTC),
            expirationDate = date
        )

        model.process(SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne)
        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it == defaultState.copy(isLoading = true) }
            .assertValueAt(2) { it == expectedState }
    }

    @Ignore("Fails on CI, works locally. Re-enable ASAP")
    @Test
    fun `update kyc state shall make interactor poll for kyc state and update the state accordingly`() {
        whenever(interactor.pollForKycState())
            .thenReturn(Single.just(SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)))

        model.process(SimpleBuyIntent.FetchKycState)

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it == defaultState.copy(kycVerificationState = KycState.PENDING) }
            .assertValueAt(2) { it == defaultState.copy(kycVerificationState = KycState.VERIFIED_AND_ELIGIBLE) }
    }

    @Ignore("Fails on CI, works locally. Re-enable ASAP")
    @Test
    fun `make card payment should update price and payment attributes`() {
        val price = FiatValue.fromMinor(
            "EUR",
            1000.toLong()
        )

        val paymentLink = "http://example.com"
        val id = "testId"
        whenever(interactor.fetchOrder(id))
            .thenReturn(
                Single.just(
                    BuySellOrder(
                        id = id,
                        pair = "EUR-BTC",
                        fiat = FiatValue.fromMinor("EUR", 10000),
                        crypto = CryptoValue.zero(CryptoCurrency.BTC),
                        state = OrderState.AWAITING_FUNDS,
                        paymentMethodId = "123-123",
                        expires = Date(),
                        price = price,
                        paymentMethodType = PaymentMethodType.PAYMENT_CARD,
                        attributes = PaymentAttributes(
                            EverypayPaymentAttrs(
                                paymentLink = paymentLink,
                                paymentState = EverypayPaymentAttrs.WAITING_3DS
                            ),
                            null,
                            null
                        ),
                        type = OrderType.BUY,
                        depositPaymentId = ""
                    )
                )
            )

        model.process(SimpleBuyIntent.MakePayment("testId"))
        model.state
            .test()
            .awaitCount(5)
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it == defaultState.copy(isLoading = true) }
            .assertValueAt(2) { it == defaultState.copy(orderExchangePrice = price) }
            .assertValueAt(3) {
                it == defaultState.copy(
                    orderExchangePrice = price,
                    everypayAuthOptions = EverypayAuthOptions(
                        paymentLink, EverypayCardActivator.redirectUrl
                    )
                )
            }
            .assertValueAt(4) { it == defaultState.copy(orderExchangePrice = price) }
    }

    @Test
    fun `polling order status with approval error should propagate`() {
        whenever(interactor.pollForOrderStatus(any())).thenReturn(
            Single.just(
                BuySellOrder(
                    id = "testId",
                    expires = Date(),
                    state = OrderState.CANCELED,
                    crypto = CryptoValue.zero(CryptoCurrency.BTC),
                    orderValue = CryptoValue.zero(CryptoCurrency.BTC),
                    paymentMethodId = "213",
                    updated = Date(),
                    paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                    fiat = FiatValue.zero("USD"),
                    pair = "USD-BTC",
                    type = OrderType.BUY,
                    depositPaymentId = "",
                    approvalErrorStatus = ApprovalErrorStatus.REJECTED
                )
            )
        )

        model.process(SimpleBuyIntent.CheckOrderStatus)

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0) { it == defaultState }
            .assertValueAt(1) { it == defaultState.copy(isLoading = true) }
            .assertValueAt(2) { it == defaultState.copy(buyErrorState = ErrorState.ApprovedBankRejected) }
    }

    @Test
    fun `WHEN eligiblePaymentMethods and getRecurringBuyEligibility success THEN observe state`() {

        val eligibleAndNextPaymentDate: EligibleAndNextPaymentRecurringBuy = mock()
        whenever(interactor.eligiblePaymentMethods("USD"))
            .thenReturn(Single.just(emptyList()))

        val eligibleNextPaymentMethodType: EligibleAndNextPaymentRecurringBuy = mock()
        whenever(getEligibilityAndNextPaymentDateUseCase(Unit))
            .thenReturn(Single.just(listOf(eligibleAndNextPaymentDate)))

        verifyNoMoreInteractions(interactor)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )

        val state2 = state1.copy(
            eligibleAndNextPaymentRecurringBuy = listOf(eligibleNextPaymentMethodType),
            isLoading = false,
            selectedPaymentMethod = null,
            paymentOptions = PaymentOptions()
        )

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))
        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
            .assertValueAt(2, state2)
    }

    @Test
    fun `WHEN eligiblePaymentMethods fails THEN observe state`() {
        whenever(interactor.eligiblePaymentMethods("USD"))
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )

        val state2 = state1.copy(
            buyErrorState = ErrorState.GenericError,
            isLoading = false,
            confirmationActionRequested = false
        )

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
            .assertValueAt(2, state2)
    }

    @Test
    fun `WHEN eligiblePaymentMethods success and getRecurringBuyEligibility fails THEN observe state`() {
        whenever(interactor.eligiblePaymentMethods("USD"))
            .thenReturn(Single.just(mock()))

        whenever(getEligibilityAndNextPaymentDateUseCase(Unit))
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod("USD", "123-321"))

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
    }
}
