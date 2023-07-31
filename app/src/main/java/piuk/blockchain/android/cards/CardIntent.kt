package piuk.blockchain.android.cards

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethod

sealed class CardIntent : MviIntent<CardState> {

    object ShowCardCreationError : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(showCardCreationError = true)
    }

    object ErrorHandled : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(showCardCreationError = false)
    }

    object CheckTokenizer : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(isLoading = true)
    }

    data class TokenizerLoaded(
        private val isVgsEnabled: Boolean,
        private val cardTokenId: String? = null,
        private val vaultId: String? = null
    ) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(
                isLoading = false,
                isVgsEnabled = isVgsEnabled,
                cardTokenId = cardTokenId,
                vaultId = vaultId
            )
    }

    object SubmitVgsCardInfo : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(vgsTokenResponse = null)
    }

    data class VgsCardInfoReceived(private val cardInfo: String) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(vgsTokenResponse = cardInfo)
    }

    class UpdateBillingAddress(private val billingAddress: BillingAddress) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(billingAddress = billingAddress)
    }

    class CardUpdated(val cardDetails: PaymentMethod.Card) : CardIntent() {
        override fun reduce(oldState: CardState): CardState = oldState.copy(cardStatus = cardDetails.status)
    }

    class ActivateCard(val card: CardData, val cardId: String) : CardIntent() {
        override fun reduce(oldState: CardState): CardState = oldState
    }

    class AuthoriseCard(private val credentials: CardAcquirerCredentials) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(authoriseCard = credentials)
    }

    class UpdateCardId(private val cardId: String) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(cardId = cardId)
    }

    class AddNewCard(val cardData: CardData) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState

        override fun isValidFor(oldState: CardState): Boolean {
            return oldState.billingAddress != null
        }
    }

    class UpdateRequestState(private val status: CardRequestStatus) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(cardRequestStatus = status)
    }

    object ResetCardAuth : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(authoriseCard = null)
    }

    object ReadyToAddNewCard : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(addCard = true)
    }

    object CardAddRequested : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(addCard = false)
    }

    data class CheckCardStatus(val vgsBeneficiaryId: String? = null) : CardIntent() {
        override fun reduce(oldState: CardState): CardState = oldState
    }

    object LoadLinkedCards : CardIntent() {
        override fun reduce(oldState: CardState): CardState = oldState
    }

    class LinkedCardsLoaded(private val linkedCards: List<LinkedPaymentMethod.Card>) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(linkedCards = linkedCards)
    }

    class CheckProviderFailureRate(val bin: String) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(isCardRejectionStateLoading = true, bin = bin, cardRejectionState = null)

        override fun isValidFor(oldState: CardState): Boolean =
            oldState.bin != bin
    }

    class UpdateCardRejectionState(private val state: CardRejectionState) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(isCardRejectionStateLoading = false, cardRejectionState = state)
    }

    object ResetCardRejectionState : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(cardRejectionState = null)
    }

    object LoadListOfUsStates : CardIntent() {
        override fun reduce(oldState: CardState): CardState = oldState
    }

    data class UsStatesLoaded(val states: List<Region.State>) : CardIntent() {
        override fun reduce(oldState: CardState): CardState =
            oldState.copy(usStateList = states)
    }
}
