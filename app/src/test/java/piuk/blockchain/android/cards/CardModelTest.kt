package piuk.blockchain.android.cards

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.braintreepayments.cardform.utils.CardType
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Date
import kotlinx.serialization.json.Json
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

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        val cardStateString = "{ mock card state }"
        whenever(sbPrefs.cardState()).thenReturn(cardStateString)
        doNothing().whenever(sbPrefs).updateCardState(anyString())

        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(FiatCurrency.Dollars)

        defaultState = spy(CardState(fiatCurrency = FiatCurrency.Dollars, billingAddress = mock(), cardId = "123"))

        val gson: Gson = mock {
            on { fromJson(sbPrefs.cardState(), CardState::class.java) }.thenReturn(defaultState)
            on { toJson(any<CardState>()) }.thenReturn(cardStateString)
        }

        val json = Json {
            ignoreUnknownKeys = true
        }

        val replaceGsonKtxFF: FeatureFlag = mock {
            on { enabled }.thenReturn(Single.just(true))
        }

        model = CardModel(
            uiScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            interactor = interactor,
            cardActivator = cardActivator,
            currencyPrefs = currencyPrefs,
            prefs = sbPrefs,
            gson = gson,
            json = json,
            replaceGsonKtxFF = replaceGsonKtxFF
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
            Partner.CARDPROVIDER, cardId
        )

        whenever(interactor.addNewCard(any(), any(), any())).thenReturn(
            Single.just(cardToBeActivated)
        )

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
        val billingAddress: BillingAddress = mock()
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
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.CARD_CREATE_NO_TOKEN
        }
    }

    @Test
    fun `add new card fails with other exception`() {
        val cardData: CardData = mock()
        val billingAddress: BillingAddress = mock()
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
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.CREATION_FAILED
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
                paymentLink, exitLink
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
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.INSUFFICIENT_CARD_BALANCE
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
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.ACTIVATION_FAIL
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
            cardType = CardType.HIPERCARD.name,
            status = CardStatus.ACTIVE,
            isEligible = true
        )

        val returnedIntent = CardIntent.CardUpdated(card)
        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.just(returnedIntent)
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus)

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
            cardType = CardType.HIPERCARD.name,
            status = CardStatus.BLOCKED,
            isEligible = true
        )

        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.just(CardIntent.CardUpdated(card))
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardStatus == card.status
        }.assertValueAt(3) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.LINK_FAILED
        }
    }

    @Test
    fun `check card status fails`() {
        whenever(interactor.pollForCardStatus(defaultState.cardId!!)).thenReturn(
            Single.error(Exception())
        )

        val test = model.state.test()
        model.process(CardIntent.CheckCardStatus)

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.cardRequestStatus == CardRequestStatus.Loading
        }.assertValueAt(2) {
            it.cardRequestStatus is CardRequestStatus.Error &&
                (it.cardRequestStatus as CardRequestStatus.Error).type == CardError.PENDING_AFTER_POLL
        }
    }
}
