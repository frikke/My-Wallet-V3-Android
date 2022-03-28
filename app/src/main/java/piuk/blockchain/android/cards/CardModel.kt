package piuk.blockchain.android.cards

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.google.gson.Gson
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.cards.partners.CompleteCardActivation
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor

class CardModel(
    uiScheduler: Scheduler,
    currencyPrefs: CurrencyPrefs,
    private val interactor: SimpleBuyInteractor,
    private val prefs: SimpleBuyPrefs,
    private val cardActivator: CardActivator,
    private val gson: Gson,
    private val json: Json,
    private val replaceGsonKtxFF: FeatureFlag,
    val environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<CardState, CardIntent>(
    initialState = prefs.cardState()?.run {
        if (replaceGsonKtxFF.isEnabled) json.decodeFromString<CardState>(this)
        else gson.fromJson(this, CardState::class.java)
    } ?: CardState(fiatCurrency = currencyPrefs.selectedFiatCurrency),
    uiScheduler = uiScheduler,
    environmentConfig = environmentConfig,
    crashLogger = crashLogger
) {

    override fun performAction(previousState: CardState, intent: CardIntent): Disposable? =
        when (intent) {
            is CardIntent.AddNewCard -> handleAddNewCard(intent, previousState)
            is CardIntent.ActivateCard -> activateCard(intent)
            is CardIntent.CheckCardStatus -> checkCardStatus(previousState)
            else -> null
        }

    private fun handleAddNewCard(
        intent: CardIntent.AddNewCard,
        previousState: CardState
    ) = interactor.addNewCard(
        intent.cardData,
        previousState.fiatCurrency,
        previousState.billingAddress
            ?: throw IllegalStateException("No billing address was provided")
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }.subscribeBy(
            onSuccess = { card ->
                process(
                    CardIntent.ActivateCard(
                        cardId = card.cardId,
                        card = intent.cardData
                    )
                )
                process(CardIntent.UpdateCardId(card.cardId))
            },
            onError = {
                process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.CREATION_FAILED)))
            }
        )

    private fun activateCard(intent: CardIntent.ActivateCard) = cardActivator.activateCard(
        intent.card,
        intent.cardId
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }.subscribeBy(
            onSuccess = {
                process(
                    CardIntent.AuthoriseCard(
                        credentials = it.toCardAcquirerCredentials()
                    )
                )
            },
            onError = {
                if (it is NabuApiException) {
                    when (it.getErrorCode()) {
                        NabuErrorCodes.InsufficientCardFunds -> process(
                            CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.INSUFFICIENT_CARD_BALANCE))
                        )
                        NabuErrorCodes.CardPaymentDeclined -> process(
                            CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.CARD_PAYMENT_DECLINED))
                        )
                        else -> process(
                            CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.ACTIVATION_FAIL))
                        )
                    }
                } else {
                    process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.ACTIVATION_FAIL)))
                }
            }
        )

    private fun checkCardStatus(previousState: CardState) = interactor.pollForCardStatus(
        previousState.cardId
            ?: throw IllegalStateException("No card ID was provided")
    )
        .doOnSubscribe {
            process(CardIntent.UpdateRequestState(CardRequestStatus.Loading))
        }
        .subscribeBy(
            onSuccess = {
                process(it)
                if (it.cardDetails.status == CardStatus.ACTIVE) {
                    process(
                        CardIntent.UpdateRequestState(
                            CardRequestStatus.Success(
                                it.cardDetails
                            )
                        )
                    )
                } else {
                    process(
                        CardIntent.UpdateRequestState(
                            CardRequestStatus.Error(
                                if (it.cardDetails.status == CardStatus.PENDING) CardError.PENDING_AFTER_POLL
                                else CardError.LINK_FAILED
                            )
                        )
                    )
                }
            },
            onError = {
                process(CardIntent.UpdateRequestState(CardRequestStatus.Error(CardError.PENDING_AFTER_POLL)))
            }
        )

    private fun CompleteCardActivation.toCardAcquirerCredentials() = when (this) {
        is CompleteCardActivation.EverypayCompleteCardActivationDetails ->
            CardAcquirerCredentials.Everypay(paymentLink, exitLink)
        is CompleteCardActivation.StripeCardActivationDetails ->
            CardAcquirerCredentials.Stripe(
                apiKey,
                clientSecret
            )
        is CompleteCardActivation.CheckoutCardActivationDetails -> {
            CardAcquirerCredentials.Checkout(
                apiKey,
                paymentLink,
                exitLink
            )
        }
    }

    override fun onStateUpdate(s: CardState) {
        prefs.updateCardState(
            if (replaceGsonKtxFF.isEnabled) json.encodeToString(s)
            else gson.toJson(s)
        )
    }
}
