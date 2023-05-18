package piuk.blockchain.android.cards.cvv

import androidx.lifecycle.viewModelScope
import com.blockchain.api.NabuApiException
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.CardType
import com.blockchain.extensions.exhaustive
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.mapper.icon

data class SecurityCodeModelState(
    val isCardDetailsLoading: Boolean = true,
    val isSecurityCodeUpdating: Boolean = false,
    val paymentId: String? = null,
    val cvvValue: String = "",
    val cardName: String? = null,
    val lastCardDigits: String? = null,
    val cardType: String? = null,
    val error: UpdateSecurityCodeError? = null
) : ModelState

sealed class SecurityCodeNavigation : NavigationEvent {
    object Back : SecurityCodeNavigation()
    object Next : SecurityCodeNavigation()
    data class FinishWithUxError(val serverSideErrorInfo: ServerSideUxErrorInfo) : SecurityCodeNavigation()
}

sealed class UpdateSecurityCodeError {
    data class CardDetailsFailed(val message: String?) : UpdateSecurityCodeError()
    data class UpdateCvvFailed(val message: String?) : UpdateSecurityCodeError()
}

@Parcelize
data class SecurityCodeArgs(
    val paymentId: String,
    val cardId: String
) : ModelConfigArgs.ParcelableArgs

class SecurityCodeViewModel(
    private val paymentMethodsService: PaymentMethodsService
) : MviViewModel<
    SecurityCodeIntent,
    SecurityCodeViewState,
    SecurityCodeModelState,
    SecurityCodeNavigation,
    SecurityCodeArgs
    >(SecurityCodeModelState()) {

    override fun viewCreated(args: SecurityCodeArgs) {
        updateState { copy(paymentId = args.paymentId) }

        viewModelScope.launch {
            paymentMethodsService.getCardDetailsCo(args.cardId)
                .doOnSuccess { cardInfo ->
                    updateState {
                        copy(
                            isCardDetailsLoading = false,
                            cardName = cardInfo.card?.label,
                            lastCardDigits = cardInfo.card?.number,
                            cardType = cardInfo.card?.type,
                            error = null
                        )
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            isCardDetailsLoading = false,
                            error = UpdateSecurityCodeError.CardDetailsFailed(error.message)
                        )
                    }
                }
        }
    }

    override fun SecurityCodeModelState.reduce() = SecurityCodeViewState(
        cardDetailsLoading = isCardDetailsLoading,
        nextButtonState = when {
            isCardDetailsLoading || cvvValue.length < 3 -> ButtonState.Disabled
            isSecurityCodeUpdating -> ButtonState.Loading
            else -> ButtonState.Enabled
        },
        cvv = cvvValue,
        cvvLength = if (cardType?.toCardType() == CardType.AMEX) 4 else 3,
        cardName = cardName ?: cardType,
        lastCardDigits = lastCardDigits,
        cardIcon = ImageResource.Local(
            id = cardType?.toCardType()?.icon() ?: R.drawable.ic_card_icon,
            contentDescription = null
        ),
        error = error
    )

    override suspend fun handleIntent(modelState: SecurityCodeModelState, intent: SecurityCodeIntent) {
        when (intent) {
            is SecurityCodeIntent.CvvInputChanged -> updateState { copy(cvvValue = intent.cvvValue) }
            SecurityCodeIntent.NextClicked -> updateCvv()
        }.exhaustive
    }

    private fun updateCvv() {
        modelState.paymentId?.let { paymentId ->
            updateState { copy(isSecurityCodeUpdating = true) }
            viewModelScope.launch {
                paymentMethodsService.updateCvv(paymentId = paymentId, cvv = modelState.cvvValue)
                    .doOnSuccess {
                        updateState {
                            copy(
                                isSecurityCodeUpdating = false,
                                error = null
                            )
                        }
                        navigate(SecurityCodeNavigation.Next)
                    }
                    .doOnFailure { error ->
                        if (error is NabuApiException && error.getServerSideErrorInfo() != null) {
                            navigate(SecurityCodeNavigation.FinishWithUxError(error.getServerSideErrorInfo()!!))
                        } else {
                            updateState {
                                copy(
                                    isSecurityCodeUpdating = false,
                                    error = UpdateSecurityCodeError.UpdateCvvFailed(error.message)
                                )
                            }
                        }
                    }
            }
        }
    }

    private fun String.toCardType(): CardType = try {
        CardType.valueOf(this)
    } catch (ex: Exception) {
        CardType.UNKNOWN
    }
}
