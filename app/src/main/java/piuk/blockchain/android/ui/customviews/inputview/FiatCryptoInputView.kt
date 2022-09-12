package piuk.blockchain.android.ui.customviews.inputview

import android.content.Context
import android.text.Editable
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.viewextensions.afterMeasured
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.properties.Delegates
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.EnterFiatCryptoLayoutBinding
import piuk.blockchain.android.util.AfterTextChangedWatcher

class FiatCryptoInputView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs), KoinComponent {

    private val binding: EnterFiatCryptoLayoutBinding =
        EnterFiatCryptoLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    val onImeAction: Observable<PrefixedOrSuffixedEditText.ImeOptions> by lazy {
        binding.enterAmount.onImeAction
    }

    private val inputAmountKeyboard: InputAmountKeyboard by inject()
    private val amountSubject: PublishSubject<Money> = PublishSubject.create()
    private val exchangeSubject: BehaviorSubject<Money> = BehaviorSubject.create()

    private val inputToggleSubject: PublishSubject<Currency> = PublishSubject.create()

    val onInputToggle: Observable<Currency>
        get() = inputToggleSubject

    val amount: Observable<Money>
        get() = amountSubject.distinctUntilChanged().doOnSubscribe {
            convertAmount()
        }

    private val exchangeRates: ExchangeRatesDataManager by inject()

    private val currencyPrefs: CurrencyPrefs by inject()

    private val conversionModel: FiatCryptoConversionModel by lazy {
        FiatCryptoConversionModel(exchangeRates)
    }

    var customInternalExchangeRate: ExchangeRate = ExchangeRate.identityExchangeRate(currencyPrefs.selectedFiatCurrency)
        set(value) {
            field = value
            conversionModel.overrideInternalExchangeRate(
                value.normaliseForInputAndExchange(configuration.inputCurrency, configuration.exchangeCurrency)
            )
            convertAmount()
        }

    private val disposables = CompositeDisposable()

    init {
        with(binding) {
            addDigitsToView()
            setInputTypeToView()

            disposables += enterAmount.textSize.subscribe { textSize ->
                if (enterAmount.text.toString() == enterAmount.configuration.prefixOrSuffix) {
                    placeFakeHint(textSize, enterAmount.configuration.isPrefix)
                } else
                    fakeHint.gone()
            }

            enterAmount.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    convertAmount()
                }
            })

            currencySwap.setOnClickListener {
                val newInputAmount = exchangeSubject.value ?: Money.zero(configuration.inputCurrency)
                configuration = configuration.copy(
                    inputCurrency = configuration.exchangeCurrency,
                    outputCurrency = configuration.exchangeCurrency,
                    exchangeCurrency = configuration.inputCurrency,
                    predefinedAmount = newInputAmount
                )
                conversionModel.amountUpdated(newInputAmount)
                inputToggleSubject.onNext(configuration.inputCurrency)
            }

            disposables += conversionModel.exchangeAmount
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        updateUiAmounts(it)
                    }
                )
        }
    }

    var configured = false
        private set

    private fun addDigitsToView() {
        binding.enterAmount.keyListener = DigitsKeyListener.getInstance(inputAmountKeyboard.validInputCharacters())
    }

    private fun setInputTypeToView() {
        binding.enterAmount.inputType = inputAmountKeyboard.inputTypeForAmount()
    }

    private fun placeFakeHint(textSize: Int, hasPrefix: Boolean) {
        with(binding) {
            fakeHint.visible()
            fakeHint.afterMeasured {
                it.translationX =
                    if (hasPrefix) (enterAmount.width / 2f + textSize / 2f) +
                        resources.getDimensionPixelOffset(R.dimen.smallest_spacing) else
                        enterAmount.width / 2f - textSize / 2f - it.width -
                            resources.getDimensionPixelOffset(R.dimen.smallest_spacing)
            }
        }
    }

    private fun convertAmount() {
        conversionModel.amountUpdated(
            getLastEnteredAmount(configuration)
        )
    }

    private fun getLastEnteredAmount(configuration: FiatCryptoViewConfiguration): Money =
        binding.enterAmount.bigDecimalValue?.let { enterAmount ->
            Money.fromMajor(configuration.inputCurrency, enterAmount)
        } ?: Money.zero(configuration.inputCurrency)

    var configuration: FiatCryptoViewConfiguration by Delegates.observable(
        FiatCryptoViewConfiguration(
            inputCurrency = currencyPrefs.selectedFiatCurrency,
            outputCurrency = currencyPrefs.selectedFiatCurrency,
            exchangeCurrency = currencyPrefs.selectedFiatCurrency
        )
    ) { _, oldValue, newValue ->
        if (oldValue != newValue || !configured) {
            configured = true
            with(binding) {
                enterAmount.filters = emptyArray()

                val inputSymbol = newValue.inputCurrency.symbol
                currencySwap.visibleIf { newValue.swapEnabled }
                exchangeAmount.visibleIf { !newValue.inputIsSameAsExchange }
                exchangeAmount.goneIf { !newValue.showExchangeRate }

                maxLimit?.let { amount ->
                    disposables += conversionModel.convert(amount, newValue)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onSuccess = { updateFilters(inputSymbol, it) }
                        )
                }

                fakeHint.text = Money.zero(newValue.inputCurrency).toStringWithoutSymbol()
                enterAmount.configuration = PrefixedOrSuffixedEditText.Configuration(
                    prefixOrSuffix = inputSymbol,
                    isPrefix = newValue.inputCurrency.type == CurrencyType.FIAT,
                    initialText = newValue.predefinedAmount.toStringWithoutSymbol()
                        .replace(DecimalFormatSymbols(Locale.getDefault()).groupingSeparator.toString(), "")
                        .removeSuffix("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00")
                )
                enterAmount.resetForTyping()
                conversionModel.configUpdated(configuration)
            }
        }
    }

    var maxLimit by Delegates.observable<Money?>(null) { _, oldValue, newValue ->
        if (newValue != oldValue && newValue != null) {
            disposables += conversionModel.convert(newValue, configuration)
                .subscribeBy(
                    onSuccess = {
                        updateFilters(
                            binding.enterAmount.configuration.prefixOrSuffix,
                            it
                        )
                    }
                )
        }
    }

    @Deprecated("Error messages arent part of the input")
    fun showError(errorMessage: String, shouldDisableInput: Boolean = false) {
        with(binding) {
            error.text = errorMessage
            error.visible()
            info.gone()
            hideExchangeAmount()
            exchangeAmount.isEnabled = !shouldDisableInput
        }
    }

    fun onAmountValidationUpdated(isValid: Boolean) {
        val colour = if (isValid) R.color.grey_800 else R.color.red_400
        binding.enterAmount.setTextColor(resources.getColor(colour, null))
        binding.exchangeAmount.setTextColor(resources.getColor(colour, null))
    }

    fun showInfo(infoMessage: String, onClick: () -> Unit) {
        with(binding) {
            info.text = infoMessage
            error.gone()
            info.visible()
            info.setOnClickListener {
                onClick()
            }
            hideExchangeAmount()
        }
    }

    fun hideInfo() {
        binding.info.gone()
    }

    private fun hideExchangeAmount() {
        binding.exchangeAmount.gone()
    }

    private fun showExchangeAmount() {
        if (!configuration.inputIsSameAsExchange) {
            binding.exchangeAmount.visible()
        }
    }

    private fun showValue(money: Money) {
        configuration = configuration.copy(
            predefinedAmount = money
        )
    }

    private fun updateFilters(prefixOrSuffix: String, value: Money) {
        val maxDecimalDigitsForAmount = value.userDecimalPlaces
        val maxIntegerDigitsForAmount = value.toStringParts().major.length
        binding.enterAmount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
    }

    private fun PrefixedOrSuffixedEditText.addFilter(
        maxDecimalDigitsForAmount: Int,
        maxIntegerDigitsForAmount: Int,
        prefixOrSuffix: String
    ) {
        filters =
            arrayOf(
                DecimalDigitsInputFilter(
                    digitsAfterZero = maxDecimalDigitsForAmount,
                    prefixOrSuffix = prefixOrSuffix
                )
            )
    }

    private fun updateUiAmounts(amounts: ConvertedAmounts) {
        with(binding) {
            exchangeAmount.text = amounts.exchangeAmount.toStringWithSymbol()
            exchangeSubject.onNext(amounts.exchangeAmount)
            val shouldResetConfig = amounts.outputAmount.isZero && getLastEnteredAmount(configuration).isZero
            if (shouldResetConfig) {
                updateValue(amounts.outputAmount)
            }
            amountSubject.onNext(amounts.outputAmount)
        }
    }

    fun fixExchange(it: Money) {
        binding.exchangeAmount.text = it.toStringWithSymbol()
    }

    fun canEdit(canEdit: Boolean) {
        binding.enterAmount.isEnabled = canEdit
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }

    fun updateValue(amount: Money) {
        if (configuration.inputCurrency != amount.currency) {
            configuration = configuration.copy(
                inputCurrency = amount.currency,
                exchangeCurrency = configuration.inputCurrency,
                outputCurrency = amount.currency
            )
        }
        showValue(amount)
    }

    private fun ExchangeRate.normaliseForInputAndExchange(
        inputCurrency: Currency,
        exchangeCurrency: Currency
    ): ExchangeRate {
        return when {
            from == inputCurrency && exchangeCurrency == to -> this

            from == exchangeCurrency && to == inputCurrency ->
                return this.inverse()

            else -> throw IllegalArgumentException("Invalid exchange Rate $this")
        }
    }
}

data class FiatCryptoViewConfiguration(
    val inputCurrency: Currency, // the currency used for input by the user
    val exchangeCurrency: Currency, // the currency used for the exchanged amount
    val outputCurrency: Currency = inputCurrency, // the currency used for the model output
    val predefinedAmount: Money = Money.zero(inputCurrency),
    val canSwap: Boolean = true,
    val showExchangeRate: Boolean = true
) {
    val inputIsSameAsExchange: Boolean
        get() = inputCurrency == exchangeCurrency

    val swapEnabled: Boolean
        get() = canSwap && inputCurrency != exchangeCurrency
}
