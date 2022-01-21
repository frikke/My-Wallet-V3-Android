package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.blockchain.commonarch.presentation.mvi.MviFragment
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.preferences.SimpleBuyPrefs
import com.braintreepayments.cardform.utils.CardType
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Calendar
import java.util.Date
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.util.AfterTextChangedWatcher

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(), AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    private var availableCards: List<LinkedPaymentMethod.Card> = emptyList()
    private val compositeDisposable = CompositeDisposable()
    private val paymentsDataManager: PaymentsDataManager by scopedInject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()

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

            compositeDisposable += paymentsDataManager.getLinkedCards(
                CardStatus.PENDING,
                CardStatus.ACTIVE
            ).subscribeBy(onSuccess = {
                availableCards = it
            })
            cardNumber.displayCardTypeIcon(false)
        }
        activity.updateToolbarTitle(getString(R.string.add_card_title))
        analytics.logEvent(SimpleBuyAnalytics.ADD_CARD)

        setupCardInfo()
    }

    private fun setupCardInfo() {
        if (simpleBuyPrefs.addCardInfoDismissed) {
            binding.cardInfoGroup.gone()
        } else {
            binding.cardInfoClose.setOnClickListener {
                simpleBuyPrefs.addCardInfoDismissed = true
                binding.cardInfoGroup.gone()
            }
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
                    cardNumber.cardType == it.cardType
                )
                    return true
            }
            return false
        }
    }

    private fun showError() {
        binding.sameCardError.visible()
    }

    override fun render(newState: CardState) {}

    override fun onBackPressed(): Boolean = true

    private fun Date.hasSameMonthAndYear(year: Int, month: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        // calendar api returns months 0-11
        return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month - 1
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun Int.asCalendarYear(): Int =
        if (this < 100) 2000 + this else this
}
