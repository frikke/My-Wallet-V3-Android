package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.utils.VibrationManager
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.koin.scopedInject
import com.braintreepayments.cardform.utils.CardType
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.Contextual
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.views.CardNumberEditText
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.urllinks.URL_CREDIT_CARD_FAILURES
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.openUrl

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(),
    AddCardFlowFragment,
    ErrorSlidingBottomDialog.Host {

    override val model: CardModel by scopedInject()

    private var availableCards: List<LinkedPaymentMethod.Card> = emptyList()
    private var secondaryCtaLink: String = ""
    private var cardRejectionState: CardRejectionState? = null

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddNewCardBinding =
        FragmentAddNewCardBinding.inflate(inflater, container, false)

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    private val cardTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            with(binding) {
                with(cardNumber) {
                    if (isError) {
                        resetCardRejectionState()
                    }

                    text?.let { input ->
                        if (input.length == CARD_BIN_LENGTH) {
                            model.process(CardIntent.CheckProviderFailureRate(input.toString()))
                        } else if (input.length < CARD_BIN_LENGTH) {
                            model.process(CardIntent.ResetCardRejectionState)
                        }
                    }
                }

                checkAllFieldsValidity()
            }
            hideError()
        }
    }

    private val otherFieldsTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            with(binding) {
                checkAllFieldsValidity()
            }
            hideError()
        }
    }

    private val cardTypeWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                with(binding) {
                    when (cardNumber.cardType) {
                        CardType.MASTERCARD -> {
                            cardCvvInput.hint = getString(R.string.card_cvc)
                            cvv.setErrorMessage(R.string.invalid_cvc)
                        }
                        else -> {
                            cardCvvInput.hint = getString(R.string.card_cvv)
                            cvv.setErrorMessage(R.string.invalid_cvv)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        model.process(CardIntent.LoadLinkedCards)

        with(binding) {
            cardName.addTextChangedListener(otherFieldsTextWatcher)
            cvv.addTextChangedListener(otherFieldsTextWatcher)
            expiryDate.addTextChangedListener(otherFieldsTextWatcher)

            cardNumber.apply {
                addTextChangedListener(cardTypeWatcher)
                addTextChangedListener(cardTextWatcher)
                attachListener(object : CardNumberEditText.CardNumberListener {
                    override fun onPaste() {
                        model.process(CardIntent.CheckProviderFailureRate(cardNumber.text.toString()))
                    }

                    override fun onCut() {
                        // do nothing
                    }

                    override fun onCopy() {
                        // do nothing
                    }
                })
            }

            btnNext.apply {
                text = getString(R.string.common_next)
                buttonState = ButtonState.Disabled
                onClick = {
                    if (cardHasAlreadyBeenAdded()) {
                        showError()
                    } else {
                        cardDetailsPersistence.setCardData(
                            CardData(
                                fullName = cardName.text.toString(),
                                number = cardNumber.text.toString().replace(" ", ""),
                                month = expiryDate.month.toInt(),
                                year = expiryDate.year.toInt(),
                                cvv = cvv.text.toString()
                            )
                        )
                        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                        navigator.navigateToBillingDetails()
                        analytics.logEvent(SimpleBuyAnalytics.CARD_INFO_SET)
                    }
                }
            }

            cardNumber.displayCardTypeIcon(false)

            setupCardInfo()
        }
        activity.updateToolbarTitle(getString(R.string.add_card_title))
        analytics.logEvent(SimpleBuyAnalytics.ADD_CARD)
    }

    override fun render(newState: CardState) {
        newState.linkedCards?.let {
            availableCards = it
        }

        newState.cardRejectionState?.let { state ->
            cardRejectionState = state

            handleCardRejectionState(state)
        } ?: run {
            resetCardRejectionState()
        }
    }

    private fun hideError() {
        binding.sameCardError.gone()
    }

    private fun FragmentAddNewCardBinding.setupCardInfo() {
        creditCardDisclaimer.apply {
            title = getString(R.string.card_info_title)
            subtitle = getString(R.string.card_info_description)
            isDismissable = false
            primaryCta = CardButton(
                text = getString(R.string.common_learn_more),
                onClick = {
                    openUrl(URL_CREDIT_CARD_FAILURES)
                }
            )
        }
    }

    private fun cardHasAlreadyBeenAdded(): Boolean {
        with(binding) {
            availableCards.forEach {
                if (it.expireDate.hasSameMonthAndYear(
                        month = expiryDate.month.toInt(),
                        year = expiryDate.year.toInt().asCalendarYear()
                    ) &&
                    cardNumber.text?.toString()?.takeLast(4) == it.endDigits &&
                    cardNumber.cardType.name == it.cardType
                )
                    return true
            }
            return false
        }
    }

    private fun showError() {
        binding.sameCardError.visible()
    }

    private fun resetCardRejectionState() {
        with(binding) {
            cardInputAlert.gone()
            cardInputAlertInfo.gone()
        }
    }

    private fun handleCardRejectionState(state: @Contextual CardRejectionState) {
        with(binding) {
            when (state) {
                is CardRejectionState.AlwaysRejected -> {
                    btnNext.buttonState = ButtonState.Disabled

                    val actionTitle = state.title ?: getString(R.string.card_issuer_always_rejects_title)
                    val ctaCopies = getActionTexts(state.actions)

                    VibrationManager.vibrate(requireContext(), intensity = VibrationManager.VibrationIntensity.High)

                    showCardRejectionAlert(title = actionTitle, isError = true)
                    showCardRejectionLearnMore(
                        title = actionTitle,
                        description = state.description ?: getString(R.string.card_issuer_always_rejects_desc),
                        primaryCtaText = ctaCopies.first,
                        secondaryCtaText = ctaCopies.second,
                        iconUrl = state.iconUrl,
                        statusIconUrl = state.statusIconUrl,
                        analyticsCategories = state.analyticsCategories
                    )
                }
                is CardRejectionState.MaybeRejected -> {
                    if (isCardInfoDataValid()) {
                        btnNext.buttonState = ButtonState.Enabled
                    }

                    val actionTitle = state.title ?: getString(R.string.card_issuer_sometimes_rejects_title)
                    val ctaCopies = getActionTexts(state.actions)

                    VibrationManager.vibrate(requireContext(), intensity = VibrationManager.VibrationIntensity.Medium)

                    showCardRejectionAlert(title = actionTitle, isError = false)
                    showCardRejectionLearnMore(
                        title = actionTitle,
                        description = state.description ?: getString(R.string.card_issuer_sometimes_rejects_desc),
                        primaryCtaText = ctaCopies.first,
                        secondaryCtaText = ctaCopies.second,
                        iconUrl = state.iconUrl,
                        statusIconUrl = state.statusIconUrl,
                        analyticsCategories = state.analyticsCategories
                    )
                }
                CardRejectionState.NotRejected -> {
                    resetCardRejectionState()

                    if (isCardInfoDataValid()) {
                        btnNext.buttonState = ButtonState.Enabled
                    }
                }
            }
        }
    }

    private fun getActionTexts(actions: List<ServerErrorAction>): Pair<String, String> {
        val primaryCtaText = if (actions.isNotEmpty() && actions[0].deeplinkPath.isNotEmpty()) {
            actions[0].title
        } else {
            getString(R.string.common_ok)
        }
        val secondaryCtaText = if (actions.isNotEmpty() && actions.size == 2 && actions[1].deeplinkPath.isNotEmpty()) {
            secondaryCtaLink = actions[1].deeplinkPath
            actions[1].title
        } else {
            getString(R.string.common_ok)
        }

        return Pair(primaryCtaText, secondaryCtaText)
    }

    private fun showCardRejectionAlert(title: String, isError: Boolean) {
        binding.cardInputAlert.apply {
            style = ComposeTypographies.Caption1
            isMultiline = false
            gravity = ComposeGravities.Start
            textColor = if (isError) ComposeColors.Error else ComposeColors.Warning
            text = title
            visible()
        }
    }

    private fun showCardRejectionLearnMore(
        title: String,
        description: String,
        primaryCtaText: String,
        secondaryCtaText: String,
        iconUrl: String?,
        statusIconUrl: String?,
        analyticsCategories: List<String>
    ) {
        binding.cardInputAlertInfo.apply {
            style = ComposeTypographies.Caption1
            textColor = ComposeColors.Primary
            gravity = ComposeGravities.Start
            text = getString(R.string.common_learn_more)
            isMultiline = false
            onClick = {
                showBottomSheet(
                    ErrorSlidingBottomDialog.newInstance(
                        ErrorDialogData(
                            title = title,
                            description = description,
                            errorButtonCopies = ErrorButtonCopies(
                                primaryCtaText,
                                secondaryCtaText
                            ),
                            iconUrl = iconUrl,
                            statusIconUrl = statusIconUrl,
                            analyticsCategories = analyticsCategories
                        )
                    )
                )
            }
            visible()
        }
    }

    private fun FragmentAddNewCardBinding.isCardInfoDataValid() =
        cardName.isValid && cardNumber.isValid && cvv.isValid && expiryDate.isValid

    private fun FragmentAddNewCardBinding.checkAllFieldsValidity() =
        if (isCardInfoDataValid()) {
            if (cardRejectionState != null && cardRejectionState !is CardRejectionState.AlwaysRejected) {
                btnNext.buttonState = ButtonState.Enabled
            } else {
                btnNext.buttonState = ButtonState.Disabled
            }
        } else {
            btnNext.buttonState = ButtonState.Disabled
        }

    private fun openUrl(url: String) {
        requireContext().openUrl(url)
    }

    override fun onErrorPrimaryCta() {
        resetCardRejectionState()
        with(binding) {
            cardName.setText("")
            cardNumber.setText("")
            expiryDate.setText("")
            cvv.setText("")
        }
    }

    override fun onErrorSecondaryCta() {
        if (secondaryCtaLink.isNotEmpty()) {
            openUrl(secondaryCtaLink)
        } else {
            resetCardRejectionState()
        }
    }

    override fun onErrorTertiaryCta() {
        if (secondaryCtaLink.isNotEmpty()) {
            openUrl(secondaryCtaLink)
        } else {
            resetCardRejectionState()
        }
    }

    override fun onSheetClosed() {
        // do nothing
    }

    private fun Date.hasSameMonthAndYear(year: Int, month: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        // calendar api returns months 0-11
        return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month - 1
    }

    private fun Int.asCalendarYear(): Int =
        if (this < 100) 2000 + this else this

    companion object {
        // Card BIN - 8 digit code which can be looked up for its success rate
        private const val CARD_BIN_LENGTH = 8
    }
}
