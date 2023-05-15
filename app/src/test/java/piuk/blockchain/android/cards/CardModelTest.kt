package piuk.blockchain.android.cards

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.api.payments.data.CardTokenIdResponse
import com.blockchain.api.services.PaymentsService
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.CardType
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.Outcome
import com.blockchain.payments.vgs.VgsCardTokenizerService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
import com.blockchain.serializers.KZonedDateTimeSerializer
import com.blockchain.testutils.USD
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.mockk.coEvery
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CompleteCardActivation
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor

class CardModelTest {

    private lateinit var model: CardModel
    private lateinit var defaultState: CardState

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }
    private val interactor: SimpleBuyInteractor = mock()
    private val cardActivator: CardActivator = mock()
    private val currencyPrefs: CurrencyPrefs = mock()
    private val sbPrefs: SimpleBuyPrefs = mock()
    private val vgsCardTokenizerService: VgsCardTokenizerService = mock()
    private val paymentsService: PaymentsService = mockk(relaxed = true)
    private val vgsFeatureFlag: FeatureFlag = mock()

    val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntSerializer)
            contextual(IsoDateSerializer)
            contextual(KZonedDateTimeSerializer)
        }
    }

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val paymentCard = LinkedPaymentMethod.Card(
        cardId = "cardId",
        label = "label",
        endDigits = "endDigits",
        partner = Partner.CARDPROVIDER,
        expireDate = Calendar.getInstance().time,
        cardType = CardType.AMEX,
        status = CardStatus.ACTIVE,
        cardFundSources = listOf(),
        mobilePaymentType = MobilePaymentType.UNKNOWN,
        currency = USD
    )

    @Before
    fun setUp() {
        val cardStateString =
            """{"fiatCurrency":{"currencyCode":"USD"},"cardId":"123","billingAddress":{"countryCode":"countryCode","fullName":"fullName","addressLine1":"address1","addressLine2":"address2","city":"city","postCode":"postCode","state":"state"}}"""
        whenever(sbPrefs.cardState()).thenReturn(cardStateString)
        doNothing().whenever(sbPrefs).updateCardState(anyString())

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(FiatCurrency.Dollars)

        defaultState = spy(
            CardState(
                fiatCurrency = FiatCurrency.Dollars,
                billingAddress = BillingAddress(
                    countryCode = "countryCode",
                    fullName = "fullName",
                    addressLine1 = "address1",
                    addressLine2 = "address2",
                    city = "city",
                    postCode = "postCode",
                    state = "state"
                ),
                cardId = "123"
            )
        )

        model = CardModel(
            uiScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor,
            cardActivator = cardActivator,
            currencyPrefs = currencyPrefs,
            prefs = sbPrefs,
            json = json,
            vgsCardTokenizerService = vgsCardTokenizerService,
            paymentsService = paymentsService,
            vgsFeatureFlag = vgsFeatureFlag
        )
    }

    @Test
    fun `add new card succeeds`() {
        val cardData = CardData(
            fullName = "test",
            number = "1234",
            month = 1,
            year = 1990,
            cvv = "123"
        )

        val cardId = "1234"
        val cardToBeActivated = CardToBeActivated(
            Partner.CARDPROVIDER,
            cardId
        )

        whenever(interactor.addNewCard(any(), any(), any())).thenReturn(Single.just(cardToBeActivated))

        whenever(cardActivator.activateCard(cardData, cardId)).thenReturn(Single.error(Exception()))

        val test = model.state.test()
        model.process(CardIntent.AddNewCard(cardData))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(3) {
            it.cardId == cardId
        }
    }

    @Test
    fun `add new card fails with nabu exception`() {
        val cardData: CardData = mock()
        val billingAddress = BillingAddress(
            countryCode = "countryCode",
            fullName = "fullName",
            addressLine1 = "address1",
            addressLine2 = "address2",
            city = "city",
            postCode = "postCode",
            state = "state"
        )
        whenever(defaultState.billingAddress).thenReturn(billingAddress)

        val intent = CardIntent.AddNewCard(cardData)
        val exception: NabuApiException = mock {
            on { getErrorCode() }.thenReturn(NabuErrorCodes.CardCreateNoToken)
        }
        whenever(interactor.addNewCard(cardData, FiatCurrency.Dollars, billingAddress)).thenReturn(
            Single.error(exception)
        )

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.CardCreateNoToken
        }
    }

    @Test
    fun `add new card fails with other exception`() {
        val cardData: CardData = mock()
        val billingAddress = BillingAddress(
            countryCode = "countryCode",
            fullName = "fullName",
            addressLine1 = "address1",
            addressLine2 = "address2",
            city = "city",
            postCode = "postCode",
            state = "state"
        )
        whenever(defaultState.billingAddress).thenReturn(billingAddress)

        val intent = CardIntent.AddNewCard(cardData)
        whenever(interactor.addNewCard(cardData, FiatCurrency.Dollars, billingAddress)).thenReturn(
            Single.error(Exception())
        )

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.CreationFailed
        }
    }

    @Test
    fun `activate card succeeds`() {
        val cardData = CardData(
            fullName = "test",
            number = "1234",
            month = 1,
            year = 1990,
            cvv = "123"
        )

        val cardId = "1234"
        val intent = CardIntent.ActivateCard(
            card = cardData,
            cardId = cardId
        )
        val paymentLink = "paymentLink"
        val exitLink = "exitLink"
        val completeCardActivation: CompleteCardActivation =
            CompleteCardActivation.EverypayCompleteCardActivationDetails(
                paymentLink,
                exitLink
            )

        whenever(cardActivator.activateCard(cardData, cardId)).thenReturn(Single.just(completeCardActivation))
        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            (it.authoriseCard is CardAcquirerCredentials.Everypay) &&
                (it.authoriseCard as CardAcquirerCredentials.Everypay).paymentLink == paymentLink &&
                (it.authoriseCard as CardAcquirerCredentials.Everypay).exitLink == exitLink
        }
    }

    @Test
    fun `activate new card fails with nabu exception`() {
        val cardData = CardData(
            fullName = "test",
            number = "1234",
            month = 1,
            year = 1990,
            cvv = "123"
        )

        val cardId = "1234"
        val intent = CardIntent.ActivateCard(
            card = cardData,
            cardId = cardId
        )
        val exception: NabuApiException = mock {
            on { getErrorCode() }.thenReturn(NabuErrorCodes.InsufficientCardFunds)
        }
        whenever(cardActivator.activateCard(cardData, cardId)).thenReturn(Single.error(exception))

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            (it.cardRequestStatus is CardRequestStatus.Error) &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.InsufficientCardBalance
        }
    }

    @Test
    fun `activate new card fails with other exception`() {
        val cardData = CardData(
            fullName = "test",
            number = "1234",
            month = 1,
            year = 1990,
            cvv = "123"
        )

        val cardId = "1234"
        val intent = CardIntent.ActivateCard(
            card = cardData,
            cardId = cardId
        )
        val exception: Exception = mock()
        whenever(cardActivator.activateCard(cardData, cardId)).thenReturn(Single.error(exception))

        val test = model.state.test()
        model.process(intent)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            (it.cardRequestStatus is CardRequestStatus.Error) &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.ActivationFailed
        }
    }

    @Test
    fun `check card status with beneficiaryId polls for card`() {
        val beneficiaryId = "beneficiaryId"
        val returnedIntent = CardIntent.CardUpdated(mock())
        whenever(interactor.pollForCardStatus(beneficiaryId)).thenReturn(Single.just(returnedIntent))

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus(beneficiaryId))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }
    }

    @Test
    fun `check card status succeeds with active card`() {
        val card = PaymentMethod.Card(
            cardId = "123",
            limits = mock(),
            label = "label",
            endDigits = "1234",
            partner = Partner.CARDPROVIDER,
            expireDate = Date(),
            cardType = CardType.HIPERCARD,
            status = CardStatus.ACTIVE,
            isEligible = true
        )

        val returnedIntent = CardIntent.CardUpdated(card)
        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.just(returnedIntent)
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus())

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardStatus == card.status
        }.assertValueAt(3) {
            it.cardRequestStatus is CardRequestStatus.Success &&
                (it.cardRequestStatus as CardRequestStatus.Success).card == card
        }
    }

    @Test
    fun `check card status succeeds with inactive card`() {
        val card = PaymentMethod.Card(
            cardId = "123",
            limits = mock(),
            label = "label",
            endDigits = "1234",
            partner = Partner.CARDPROVIDER,
            expireDate = Date(),
            cardType = CardType.HIPERCARD,
            status = CardStatus.BLOCKED,
            isEligible = true
        )

        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.just(CardIntent.CardUpdated(card))
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus())

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardStatus == card.status
        }.assertValueAt(3) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.LinkFailed
        }
    }

    @Test
    fun `check card status fails`() {
        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.error(Exception())
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus())

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.PendingAfterPoll
        }
    }

    @Test
    fun `getting active cards returns list`() {
        val expectedCards = listOf(paymentCard)
        whenever(interactor.loadLinkedCards()).thenReturn(Single.just(expectedCards))

        val test = model.state.test()
        model.process(CardIntent.LoadLinkedCards)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.linkedCards == expectedCards
        }
    }

    @Test
    fun `when card rejection state returns always rejected then state is updated`() {
        val binNumber = "1234"
        val expectedResult = CardRejectionState.AlwaysRejected(
            ServerSideUxErrorInfo(
                id = "errorId",
                title = "title",
                description = "description",
                actions = emptyList(),
                iconUrl = "",
                statusUrl = "",
                categories = emptyList()
            )
        )
        whenever(interactor.checkNewCardRejectionRate(binNumber)).thenReturn(
            Single.just(expectedResult)
        )

        val test = model.state.test()
        model.process(CardIntent.CheckProviderFailureRate(binNumber))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(2) {
            it.cardRejectionState == expectedResult
        }
    }

    @Test
    fun `when card rejection state returns sometimes rejected then state is updated`() {
        val binNumber = "1234"
        val expectedResult = CardRejectionState.MaybeRejected(
            ServerSideUxErrorInfo(
                id = "errorId",
                title = "title",
                description = "description",
                actions = emptyList(),
                iconUrl = "",
                statusUrl = "",
                categories = emptyList()
            )
        )
        whenever(interactor.checkNewCardRejectionRate(binNumber)).thenReturn(
            Single.just(expectedResult)
        )

        val test = model.state.test()
        model.process(CardIntent.CheckProviderFailureRate(binNumber))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(2) {
            it.cardRejectionState == expectedResult
        }
    }

    @Test
    fun `when card rejection state returns not rejected then state is updated`() {
        val binNumber = "1234"
        val expectedResult = CardRejectionState.NotRejected
        whenever(interactor.checkNewCardRejectionRate(binNumber)).thenReturn(
            Single.just(expectedResult)
        )

        val test = model.state.test()
        model.process(CardIntent.CheckProviderFailureRate(binNumber))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(2) {
            it.cardRejectionState == expectedResult
        }
    }

    @Test
    fun `when card rejection state errors then state shows not rejected`() {
        val binNumber = "1234"
        whenever(interactor.checkNewCardRejectionRate(binNumber)).thenReturn(
            Single.error(Exception())
        )

        val test = model.state.test()
        model.process(CardIntent.CheckProviderFailureRate(binNumber))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.isCardRejectionStateLoading && it.bin == binNumber && it.cardRejectionState == null
        }.assertValueAt(2) {
            it.cardRejectionState is CardRejectionState.NotRejected
        }
    }

    @Test
    fun `when card tokenizer data is retrieved then state shows tokenization info`() {
        val vgsFeatureFlagEnabled = true
        val cardTokenIdResponse = CardTokenIdResponse("tokenId", "publicKey")
        whenever(vgsFeatureFlag.enabled).thenReturn(Single.just(vgsFeatureFlagEnabled))
        coEvery { paymentsService.getCardTokenId() } returns Outcome.Success(cardTokenIdResponse)

        val test = model.state.test()
        model.process(CardIntent.CheckTokenizer)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(2) {
            it.cardTokenId == cardTokenIdResponse.cardTokenId &&
                it.vaultId == cardTokenIdResponse.vgsVaultId &&
                it.isVgsEnabled == vgsFeatureFlagEnabled
        }
    }
}
