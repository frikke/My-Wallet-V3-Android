package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.basic.ComposeColors
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
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.BottomSheetInformation
import piuk.blockchain.android.ui.base.ErrorButtonCopies
import piuk.blockchain.android.ui.base.ErrorDialogData
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.urllinks.URL_CREDIT_CARD_FAILURES
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.openUrl

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(),
    AddCardFlowFragment,
    BottomSheetInformation.Host {

    override val model: CardModel by scopedInject()

    private var availableCards: List<LinkedPaymentMethod.Card> = emptyList()
    private var secondaryCtaLink: String = ""

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddNewCardBinding =
        FragmentAddNewCardBinding.inflate(inflater, container, false)

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    private val textWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            with(binding) {
                if (cardNumber.isError) {
                    resetCardRejectionState()
                }

                btnNext.buttonState = if (cardName.isValid && cardNumber.isValid && cvv.isValid && expiryDate.isValid) {
                    ButtonState.Enabled
                } else {
                    ButtonState.Disabled
                }
            }
            hideError()
        }
    }

    private val cardNumberTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            s?.let { input ->
                when {
                    input.length == CARD_BIN_LENGTH -> {
                        model.process(CardIntent.CheckProviderFailureRate(s.toString()))
                    }
                    input.length < CARD_BIN_LENGTH -> {
                        model.process(CardIntent.ResetCardRejectionState)
                    }
                }
            }
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

    private fun hideError() {
        binding.sameCardError.gone()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        model.process(CardIntent.LoadLinkedCards)

        with(binding) {
            cardName.addTextChangedListener(textWatcher)
            cardNumber.apply {
                addTextChangedListener(cardTypeWatcher)
                addTextChangedListener(textWatcher)
                addTextChangedListener(cardNumberTextWatcher)
            }
            cvv.addTextChangedListener(textWatcher)
            expiryDate.addTextChangedListener(textWatcher)
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

    override fun render(newState: CardState) {
        newState.linkedCards?.let {
            availableCards = it
        }

        newState.cardRejectionState?.let { state ->
            handleCardRejectionState(state)
        } ?: run {
            resetCardRejectionState()
        }
    }

    private fun resetCardRejectionState() {
        with(binding) {
            btnNext.apply {
                visible()
                isEnabled = true
            }

            cardInputAlert.gone()
            cardInputAlertInfo.gone()
        }
    }

    private fun handleCardRejectionState(state: @Contextual CardRejectionState) {
        with(binding) {
            when (state) {
                is CardRejectionState.AlwaysRejected -> {
                    btnNext.isEnabled = false

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
            text = getString(R.string.common_learn_more)
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

    private fun openUrl(url: String) {
        requireContext().openUrl(url)
    }

    override fun onBackPressed(): Boolean = true

    override fun primaryButtonClicked() {
        resetCardRejectionState()
        with(binding) {
            cardName.setText("")
            cardNumber.setText("")
            expiryDate.setText("")
            cvv.setText("")
        }
    }

    override fun secondButtonClicked() {
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
        // Card BIN - 6 digit code which can be looked up in the success rate
        private const val CARD_BIN_LENGTH = 6
    }
}
