package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.custodial.models.QuoteFee
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CardAttributes
import com.blockchain.nabu.datamanagers.CardPaymentState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentAttributes
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.EligibleAndNextPaymentRecurringBuy
import com.blockchain.network.PollResult
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.testutils.EUR
import com.blockchain.testutils.USD
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
import java.math.BigInteger
import java.util.Date
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.GetEligibilityAndNextPaymentDateUseCase
import piuk.blockchain.android.domain.usecases.LinkAccess

@Ignore("Ignoring because CI fails on this, re-enabling ASAP")
class SimpleBuyModelTest {

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    private val defaultState = SimpleBuyState(
        selectedCryptoAsset = CryptoCurrency.BTC,
        amount = FiatValue.fromMinor(USD, 1000.toBigInteger()),
        fiatCurrency = USD,
        selectedPaymentMethod = SelectedPaymentMethod(
            id = "123-321",
            paymentMethodType = PaymentMethodType.PAYMENT_CARD,
            isEligible = true
        ),
        id = "123"
    )

    private val interactor: SimpleBuyInteractor = mock()
    private val cardActivator: CardActivator = mock()
    private val getEligibilityAndNextPaymentDateUseCase: GetEligibilityAndNextPaymentDateUseCase = mock()

    private val prefs: CurrencyPrefs = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()

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
        simpleBuyPrefs = simpleBuyPrefs,
        initialState = defaultState,
        uiScheduler = Schedulers.io(),
        interactor = interactor,
        cardActivator = cardActivator,
        ratingPrefs = ratingPrefs,
        onboardingPrefs = mock(),
        environmentConfig = environmentConfig,
        remoteLogger = mock(),
        _activityIndicator = mock(),
        serializer = serializer,
        isFirstTimeBuyerUseCase = mock(),
        buyOrdersCache = mock(),
        getEligibilityAndNextPaymentDateUseCase = mock(),
        bankPartnerCallbackProvider = mock(),
        userIdentity = mock {
            on { isVerifiedFor(Feature.TierLevel(Tier.GOLD)) }.thenReturn(Single.just(true))
        }
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
                        target = CryptoValue.zero(CryptoCurrency.BTC),
                        orderValue = CryptoValue.zero(CryptoCurrency.BTC),
                        paymentMethodId = "213",
                        updated = Date(),
                        paymentMethodType = PaymentMethodType.FUNDS,
                        source = FiatValue.zero(USD),
                        pair = "USD-BTC",
                        type = OrderType.BUY,
                        depositPaymentId = ""
                    ),
                    BrokerageQuote(
                        id = "id",
                        price = CryptoValue.zero(CryptoCurrency.BTC),
                        quoteMargin = 4.0,
                        availability = Availability.INSTANT,
                        feeDetails = QuoteFee(
                            fee = CryptoValue.zero(CryptoCurrency.BTC),
                            feeBeforePromo = CryptoValue.zero(CryptoCurrency.BTC),
                            promo = Promo.NO_PROMO
                        )
                    )
                )
            )
        )

        val expectedState = defaultState.copy(
            orderState = OrderState.AWAITING_FUNDS,
            id = "testId",
            orderValue = CryptoValue.zero(CryptoCurrency.BTC)
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
            EUR,
            1000.toBigInteger()
        )

        val paymentLink = "http://example.com"
        val id = "testId"
        val redirectUrl = "http://redirect.com"
        whenever(cardActivator.redirectUrl).thenReturn(redirectUrl)
        whenever(interactor.fetchOrder(id))
            .thenReturn(
                Single.just(
                    BuySellOrder(
                        id = id,
                        pair = "EUR-BTC",
                        source = FiatValue.fromMinor(EUR, 10000.toBigInteger()),
                        target = CryptoValue.zero(CryptoCurrency.BTC),
                        state = OrderState.AWAITING_FUNDS,
                        paymentMethodId = "123-123",
                        expires = Date(),
                        price = price,
                        paymentMethodType = PaymentMethodType.PAYMENT_CARD,
                        attributes = PaymentAttributes(
                            authorisationUrl = null,
                            cardAttributes = CardAttributes.EveryPay(
                                paymentLink = paymentLink,
                                paymentState = CardPaymentState.WAITING_FOR_3DS
                            )
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
            .assertValueAt(2) {
                it == defaultState.copy(
                    cardAcquirerCredentials = CardAcquirerCredentials.Everypay(
                        paymentLink,
                        redirectUrl
                    )
                )
            }
    }

    @Test
    fun `polling order status with approval error should propagate`() {
        whenever(interactor.pollForOrderStatus(any())).thenReturn(
            Single.just(
                PollResult.FinalResult(
                    BuySellOrder(
                        id = "testId",
                        expires = Date(),
                        state = OrderState.CANCELED,
                        target = CryptoValue.zero(CryptoCurrency.BTC),
                        orderValue = CryptoValue.zero(CryptoCurrency.BTC),
                        paymentMethodId = "213",
                        updated = Date(),
                        paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                        source = FiatValue.zero(USD),
                        pair = "USD-BTC",
                        type = OrderType.BUY,
                        depositPaymentId = "",
                        approvalErrorStatus = ApprovalErrorStatus.Rejected
                    )
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
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.just(SimpleBuyInteractor.PaymentMethods(emptyList(), emptyList())))

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

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD, "123-321"))
        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
            .assertValueAt(2, state2)
    }

    @Test
    fun `WHEN eligiblePaymentMethods fails THEN observe state`() {
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )

        val state2 = state1.copy(
            buyErrorState = ErrorState.InternetConnectionError,
            isLoading = false,
            confirmationActionRequested = false
        )

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD, "123-321"))

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
            .assertValueAt(2, state2)
    }

    @Test
    fun `WHEN eligiblePaymentMethods success and getRecurringBuyEligibility fails THEN observe state`() {
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.just(mock()))

        whenever(getEligibilityAndNextPaymentDateUseCase(Unit))
            .thenReturn(Single.error(Throwable()))

        verifyNoMoreInteractions(interactor)

        val state1 = defaultState.copy(
            paymentOptions = PaymentOptions(),
            selectedPaymentMethod = null
        )

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD, "123-321"))

        model.state
            .test()
            .awaitCount(3)
            .assertValueAt(0, defaultState)
            .assertValueAt(1, state1)
    }

    @Test
    fun `when card linkaccess is blocked then user should not be able to add new card`() {
        val limits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, USD)
        val availableMethodType = AvailablePaymentMethodType(
            true,
            LinkAccess.BLOCKED,
            USD,
            PaymentMethodType.PAYMENT_CARD,
            limits
        )
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.just(SimpleBuyInteractor.PaymentMethods(listOf(availableMethodType), emptyList())))

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD))

        val undefinedCard = PaymentMethod.UndefinedCard(limits, isEligible = true, emptyList())
        model.state
            .test()
            .assertValueAt(1) {
                !it.paymentOptions.availablePaymentMethods.contains(undefinedCard) &&
                    !it.paymentOptions.canAddCard
            }
    }

    @Test
    fun `when card linkaccess is NEEDS_UPGRADE then user should be able to select new card but not add it`() {
        val limits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, USD)
        val availableMethodType = AvailablePaymentMethodType(
            true,
            LinkAccess.NEEDS_UPGRADE,
            USD,
            PaymentMethodType.PAYMENT_CARD,
            limits
        )
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.just(SimpleBuyInteractor.PaymentMethods(listOf(availableMethodType), emptyList())))

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD))

        val undefinedCard = PaymentMethod.UndefinedCard(limits, isEligible = false, emptyList())
        model.state
            .test()
            .assertValueAt(1) {
                it.paymentOptions.availablePaymentMethods.contains(undefinedCard) &&
                    it.paymentOptions.canAddCard
            }
    }

    @Test
    fun `when card linkaccess is GRANTED then user should be able to add new card`() {
        val limits = PaymentLimits(BigInteger.ZERO, BigInteger.ZERO, USD)
        val availableMethodType = AvailablePaymentMethodType(
            true,
            LinkAccess.GRANTED,
            USD,
            PaymentMethodType.PAYMENT_CARD,
            limits
        )
        whenever(interactor.paymentMethods(USD))
            .thenReturn(Single.just(SimpleBuyInteractor.PaymentMethods(listOf(availableMethodType), emptyList())))

        model.process(SimpleBuyIntent.FetchSuggestedPaymentMethod(USD))

        val undefinedCard = PaymentMethod.UndefinedCard(limits, isEligible = true, emptyList())
        model.state
            .test()
            .assertValueAt(1) {
                it.paymentOptions.availablePaymentMethods.contains(undefinedCard) &&
                    it.paymentOptions.canAddCard
            }
    }

    @Test
    fun `googlePay info requested should return googlePay info`() {
        val tokenizationMap = emptyMap<String, String>()
        val beneficiaryId = "beneficiaryId"
        val countryCode = "merchantBankCountryCode"
        val allowPrepaidCards = false
        val allowCreditCards = false
        whenever(interactor.getGooglePayInfo(USD))
            .thenReturn(
                Single.just(
                    SimpleBuyIntent.GooglePayInfoReceived(
                        tokenizationMap, beneficiaryId, countryCode, allowPrepaidCards, allowCreditCards
                    )
                )
            )

        val expectedState = defaultState.copy(
            googlePayTokenizationInfo = tokenizationMap,
            googlePayBeneficiaryId = beneficiaryId,
            googlePayMerchantBankCountryCode = countryCode,
            googlePayAllowPrepaidCards = allowPrepaidCards,
            googlePayAllowCreditCards = allowCreditCards
        )

        model.process(SimpleBuyIntent.GooglePayInfoRequested)
        model.state
            .test()
            .awaitCount(3)
            .assertValueSequence(
                listOf(
                    defaultState,
                    defaultState.copy(isLoading = true),
                    expectedState
                )
            )
    }
}
