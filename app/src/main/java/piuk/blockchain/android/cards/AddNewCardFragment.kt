package piuk.blockchain.android.cards

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.koin.scopedInject
import com.braintreepayments.cardform.utils.CardType
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.Calendar
import java.util.Date
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.urllinks.URL_CREDIT_CARD_FAILURES
import piuk.blockchain.android.util.AfterTextChangedWatcher

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(), AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    private var availableCards: List<LinkedPaymentMethod.Card> = emptyList()

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
                btnNext.isEnabled = cardName.isValid && cardNumber.isValid && cvv.isValid && expiryDate.isValid
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
            }
            cvv.addTextChangedListener(textWatcher)
            expiryDate.addTextChangedListener(textWatcher)
            btnNext.apply {
                isEnabled = false
                setOnClickListener {
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
                text = getString(R.string.learn_more),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(URL_CREDIT_CARD_FAILURES))
                    requireContext().startActivity(intent)
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
    }

    override fun onBackPressed(): Boolean = true

    private fun Date.hasSameMonthAndYear(year: Int, month: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        // calendar api returns months 0-11
        return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month - 1
    }

    private fun Int.asCalendarYear(): Int =
        if (this < 100) 2000 + this else this
}
