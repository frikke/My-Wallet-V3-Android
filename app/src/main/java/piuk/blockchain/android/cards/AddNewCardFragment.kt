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
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.payments.vgs.VgsCardTokenizerService
import com.blockchain.presentation.koin.scopedInject
import com.braintreepayments.cardform.utils.CardType
import com.verygoodsecurity.vgscollect.core.model.state.FieldState
import com.verygoodsecurity.vgscollect.core.storage.OnFieldStateChangeListener
import com.verygoodsecurity.vgscollect.view.InputFieldView
import com.verygoodsecurity.vgscollect.view.card.validation.rules.PersonNameRule
import com.verygoodsecurity.vgscollect.widget.VGSTextInputLayout
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.Contextual
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.mapper.isEquals
import piuk.blockchain.android.cards.views.CardNumberEditText
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.base.ErrorSlidingBottomDialog
import piuk.blockchain.android.urllinks.URL_CREDIT_CARD_FAILURES
import piuk.blockchain.android.util.AfterTextChangedWatcher

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(),
    AddCardFlowFragment,
    ErrorSlidingBottomDialog.Host {

    override val model: CardModel by scopedInject()
    private val cardTokenizerService: VgsCardTokenizerService by scopedInject()

    private val fraudService: FraudService by inject()

    private var availableCards: List<LinkedPaymentMethod.Card> = emptyList()
    private var secondaryCtaLink: String = ""
    private var isCardRejectionStateLoading: Boolean = false
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
                        if (input.length >= CARD_BIN_LENGTH) {
                            model.process(CardIntent.CheckProviderFailureRate(input.toString().take(CARD_BIN_LENGTH)))
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

    private val cardTextChangedListener = object : InputFieldView.OnTextChangedListener {
        override fun onTextChange(view: InputFieldView, isEmpty: Boolean) {
            with(binding) {
                with(vgsCardNumber) {
                    binding.vgsCardInputForm.setErrorEnabled(false)
                    binding.vgsCardInputForm.setError(null)
                    if (this.getState()?.isValid == false) {
                        resetCardRejectionState()
                    }
                    getState()?.bin?.let { input ->
                        if (input.length >= CARD_BIN_LENGTH) {
                            model.process(CardIntent.CheckProviderFailureRate(input.take(CARD_BIN_LENGTH)))
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
            afterTextChangedOnOtherFields()
        }
    }

    private val otherFieldsTextChangedListener = object : InputFieldView.OnTextChangedListener {
        override fun onTextChange(view: InputFieldView, isEmpty: Boolean) {
            afterTextChangedOnOtherFields()
        }
    }

    private fun afterTextChangedOnOtherFields() {
        with(binding) {
            checkAllFieldsValidity()
        }
        hideError()
    }

    private val cardTypeWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            s?.let {
                with(binding) {
                    when (cardNumber.cardType) {
                        CardType.MASTERCARD -> {
                            cardCvvInput.hint = getString(com.blockchain.stringResources.R.string.card_cvc)
                            cvv.setErrorMessage(com.blockchain.stringResources.R.string.invalid_cvc)
                        }
                        else -> {
                            cardCvvInput.hint = getString(com.blockchain.stringResources.R.string.card_cvv)
                            cvv.setErrorMessage(com.blockchain.stringResources.R.string.invalid_cvv)
                        }
                    }
                }
            }
        }
    }

    private val cardTypeChangedListener = object : InputFieldView.OnTextChangedListener {
        override fun onTextChange(view: InputFieldView, isEmpty: Boolean) {
            with(binding) {
                if (vgsCardNumber.getState()?.cardBrand?.equals("MASTERCARD", ignoreCase = true) == true) {
                    vgsCardCvvInput.setHint(com.blockchain.stringResources.R.string.card_cvc)
                    vgsCardCvvInput.tag = getString(com.blockchain.stringResources.R.string.invalid_cvc)
                } else {
                    vgsCardCvvInput.setHint(com.blockchain.stringResources.R.string.card_cvv)
                    vgsCardCvvInput.tag = getString(com.blockchain.stringResources.R.string.invalid_cvv)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cardTokenizerService.destroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        fraudService.trackFlow(FraudFlow.CARD_LINK)

        model.process(CardIntent.CheckTokenizer)
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
                        val cardNumberValue = cardNumber.text.toString()

                        if (cardNumberValue.length >= CARD_BIN_LENGTH) {
                            model.process(
                                CardIntent.CheckProviderFailureRate(
                                    cardNumberValue.take(CARD_BIN_LENGTH)
                                )
                            )
                        }
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
                text = getString(com.blockchain.stringResources.R.string.common_next)
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
                        navigateToBillingDetails()
                    }
                }
            }

            cardNumber.displayCardTypeIcon(false)

            setupCardInfo()
        }
        activity.updateToolbarTitle(getString(com.blockchain.stringResources.R.string.add_card_title))
        analytics.logEvent(SimpleBuyAnalytics.ADD_CARD)
    }

    private fun navigateToBillingDetails() {
        fraudService.trackFlow(FraudFlow.CARD_LINK)

        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        navigator.navigateToBillingDetails()
        analytics.logEvent(SimpleBuyAnalytics.CARD_INFO_SET)
    }

    private fun addVgsErrorListeners(
        inputLayout: VGSTextInputLayout,
        inputFieldView: InputFieldView,
        error: () -> String
    ) {
        inputFieldView.addOnTextChangeListener(object : InputFieldView.OnTextChangedListener {
            override fun onTextChange(view: InputFieldView, isEmpty: Boolean) {
                inputLayout.setErrorEnabled(false)
                inputLayout.setError(null)
            }
        })
        inputFieldView.setOnFieldStateChangeListener(object : OnFieldStateChangeListener {
            override fun onStateChange(state: FieldState) {
                if (state.contentLength > 0 && state.validationErrors.isNotEmpty() && !state.hasFocus) {
                    inputLayout.setErrorEnabled(true)
                    inputLayout.setError(error())
                }
            }
        })
    }

    private fun initVgsFields() {
        with(binding.vgsCardName) {
            if (BuildConfig.DEBUG) {
                // Needed to fully use the fakecardadquirer test functionalities on the backend
                val rule = PersonNameRule.ValidationBuilder()
                    .setRegex("^[a-zA-Z0-9 ,'._-]+\$")
                    .build()

                setRule(rule)
            }
            addOnTextChangeListener(otherFieldsTextChangedListener)
            addVgsErrorListeners(binding.vgsCardNameInput, this, {
                getString(com.blockchain.stringResources.R.string.invalid_card_name)
            })
        }
        with(binding.vgsCardNumber) {
            addOnTextChangeListener(cardTypeChangedListener)
            addOnTextChangeListener(cardTextChangedListener)
            addVgsErrorListeners(binding.vgsCardInputForm, this, {
                getString(com.blockchain.stringResources.R.string.invalid_card_number)
            })
        }
        with(binding.vgsCvv) {
            addOnTextChangeListener(otherFieldsTextChangedListener)
            addVgsErrorListeners(binding.vgsCardCvvInput, this, { binding.vgsCardCvvInput.tag as String })
        }
        with(binding.vgsExpiryDate) {
            addOnTextChangeListener(otherFieldsTextChangedListener)
            addVgsErrorListeners(binding.vgsCardDateInput, this, {
                getString(com.blockchain.stringResources.R.string.invalid_date)
            })
        }
    }

    override fun render(newState: CardState) {
        renderTokenizer(newState.isLoading, newState.isVgsEnabled, newState.vaultId, newState.cardTokenId)

        newState.linkedCards?.let {
            availableCards = it
        }

        isCardRejectionStateLoading = newState.isCardRejectionStateLoading

        newState.cardRejectionState?.let { state ->
            cardRejectionState = state

            handleCardRejectionState(state)
        } ?: run {
            resetCardRejectionState()
        }
    }

    private fun renderTokenizer(isLoading: Boolean, isVgsEnabled: Boolean, vaultId: String?, cardTokenId: String?) {
        with(binding) {
            loading.visibleIf { isLoading }
            cardScrollContainer.visibleIf { !isLoading }

            cardInputGroup.visibleIf { !isVgsEnabled }
            vgsCardInputGroup.visibleIf { isVgsEnabled }

            if (isVgsEnabled && vaultId != null && cardTokenId != null) {
                if (!cardTokenizerService.isInitialised()) {
                    cardTokenizerService.init(requireContext(), vaultId)
                    initVgsFields()
                    cardTokenizerService.bindCardDetails(
                        name = binding.vgsCardName,
                        cardNumber = binding.vgsCardNumber,
                        expiration = binding.vgsExpiryDate,
                        cvv = binding.vgsCvv,
                        cardTokenId = cardTokenId
                    )
                }

                btnNext.apply {
                    text = getString(com.blockchain.stringResources.R.string.common_next)
                    onClick = {
                        navigateToBillingDetails()
                    }
                }
            }
        }
    }

    private fun hideError() {
        binding.sameCardError.gone()
    }

    private fun FragmentAddNewCardBinding.setupCardInfo() {
        creditCardDisclaimer.apply {
            title = getString(com.blockchain.stringResources.R.string.card_info_title)
            subtitle = getString(com.blockchain.stringResources.R.string.card_info_description)
            isDismissable = false
            primaryCta = CardButton(
                text = getString(com.blockchain.stringResources.R.string.common_learn_more),
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
                    cardNumber.cardType.isEquals(it.cardType)
                ) {
                    return true
                }
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

                    val error = state.error ?: ServerSideUxErrorInfo(
                        id = null,
                        title = getString(com.blockchain.stringResources.R.string.card_issuer_always_rejects_title),
                        description =
                        getString(com.blockchain.stringResources.R.string.card_issuer_always_rejects_desc),
                        iconUrl = "",
                        statusUrl = "",
                        actions = emptyList(),
                        categories = emptyList()
                    )

                    VibrationManager.vibrate(requireContext(), intensity = VibrationManager.VibrationIntensity.High)

                    showCardRejectionAlert(title = error.title, isError = true)
                    showCardRejectionLearnMore(error)
                }
                is CardRejectionState.MaybeRejected -> {
                    if (isCardInfoDataValid()) {
                        btnNext.buttonState = ButtonState.Enabled
                    }

                    val error = state.error

                    VibrationManager.vibrate(requireContext(), intensity = VibrationManager.VibrationIntensity.Medium)

                    showCardRejectionAlert(title = error.title, isError = false)
                    showCardRejectionLearnMore(error)
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

    private fun showCardRejectionLearnMore(error: ServerSideUxErrorInfo) {
        binding.cardInputAlertInfo.apply {
            style = ComposeTypographies.Caption1
            textColor = ComposeColors.Primary
            gravity = ComposeGravities.Start
            text = getString(com.blockchain.stringResources.R.string.common_learn_more)
            isMultiline = false
            onClick = {
                showBottomSheet(ErrorSlidingBottomDialog.newInstance(error))
            }
            visible()
        }
    }

    private fun FragmentAddNewCardBinding.isCardInfoDataValid() =
        (cardName.isValid && cardNumber.isValid && cvv.isValid && expiryDate.isValid) ||
            (cardTokenizerService.isInitialised() && cardTokenizerService.isValid())

    private fun FragmentAddNewCardBinding.checkAllFieldsValidity() =
        if (isCardInfoDataValid()) {
            if (!isCardRejectionStateLoading &&
                (cardRejectionState == null || cardRejectionState !is CardRejectionState.AlwaysRejected)
            ) {
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
            vgsCardName.setText("")
            cardNumber.setText("")
            vgsCardNumber.setText("")
            expiryDate.setText("")
            vgsExpiryDate.setText("")
            cvv.setText("")
            vgsCvv.setText("")
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
        // Card BIN - 6 digit code which can be looked up for its success rate
        private const val CARD_BIN_LENGTH = 6
    }
}
