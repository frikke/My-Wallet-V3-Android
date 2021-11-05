package piuk.blockchain.android.ui.customviews.inputview

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
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
import java.util.Currency
import java.util.Locale
import kotlin.properties.Delegates
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.EnterFiatCryptoLayoutBinding
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.afterMeasured
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.visibleIf

class FiatCryptoInputView(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs), KoinComponent {

    private val binding: EnterFiatCryptoLayoutBinding =
        EnterFiatCryptoLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    val onImeAction: Observable<PrefixedOrSuffixedEditText.ImeOptions> by lazy {
        binding.enterAmount.onImeAction
    }

    private val amountSubject: PublishSubject<Money> = PublishSubject.create()
    private val exchangeSubject: BehaviorSubject<Money> = BehaviorSubject.create()

    private val inputToggleSubject: PublishSubject<CurrencyType> = PublishSubject.create()

    val onInputToggle: Observable<CurrencyType>
        get() = inputToggleSubject

    val amount: Observable<Money>
        get() = amountSubject.distinctUntilChanged()

    private val exchangeRates: ExchangeRatesDataManager by inject()

    private val currencyPrefs: CurrencyPrefs by inject()

    private val conversionModel: FiatCryptoConversionModel by lazy {
        FiatCryptoConversionModel(exchangeRates)
    }

    var customInternalExchangeRate: ExchangeRate? = null
        set(value) {
            field = value
            convertAmount()
        }

    private val disposables = CompositeDisposable()

    init {
        with(binding) {
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
                val newInputAmount = exchangeSubject.value ?: configuration.inputCurrency.zeroValue()
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

    private fun placeFakeHint(textSize: Int, hasPrefix: Boolean) {
        with(binding) {
            fakeHint.visible()
            fakeHint.afterMeasured {
                it.translationX =
                    if (hasPrefix) (enterAmount.width / 2f + textSize / 2f) +
                        resources.getDimensionPixelOffset(R.dimen.smallest_margin) else
                        enterAmount.width / 2f - textSize / 2f - it.width -
                            resources.getDimensionPixelOffset(R.dimen.smallest_margin)
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
            when (configuration.inputCurrency) {
                is CurrencyType.Fiat -> FiatValue.fromMajor(configuration.inputCurrency.fiatCurrency, enterAmount)
                is CurrencyType.Crypto -> CryptoValue.fromMajor(configuration.inputCurrency.cryptoCurrency, enterAmount)
            }
        } ?: configuration.inputCurrency.zeroValue()

    var configuration: FiatCryptoViewConfiguration by Delegates.observable(
        FiatCryptoViewConfiguration(
            inputCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency),
            outputCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency),
            exchangeCurrency = CurrencyType.Fiat(currencyPrefs.selectedFiatCurrency)
        )
    ) { _, oldValue, newValue ->
        if (oldValue != newValue || !configured) {
            configured = true
            with(binding) {
                enterAmount.filters = emptyArray()

                val inputSymbol = newValue.inputCurrency.symbol()
                currencySwap.visibleIf { newValue.swapEnabled }
                exchangeAmount.visibleIf { !newValue.inputIsSameAsExchange }

                maxLimit?.let { amount ->
                    disposables += conversionModel.convert(amount, newValue)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                            onSuccess = { updateFilters(inputSymbol, it) }
                        )
                }

                fakeHint.text = newValue.inputCurrency.zeroValue().toStringWithoutSymbol()
                enterAmount.configuration = PrefixedOrSuffixedEditText.Configuration(
                    prefixOrSuffix = inputSymbol,
                    isPrefix = newValue.inputCurrency is CurrencyType.Fiat,
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

    private fun hideExchangeAmount() {
        binding.exchangeAmount.gone()
    }

    fun hideLabels() {
        binding.error.gone()
        binding.info.gone()
        showExchangeAmount()
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

            if (amounts.outputAmount.isZero) {
                updateValue(amounts.outputAmount)
            }
            amountSubject.onNext(amounts.outputAmount)
        }
    }

    fun fixExchange(it: Money) {
        binding.exchangeAmount.text = it.toStringWithSymbol()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }

    fun updateValue(amount: Money) {
        if (configuration.inputCurrency is CurrencyType.Fiat && amount is CryptoValue) {
            configuration = configuration.copy(
                inputCurrency = CurrencyType.Crypto(amount.currency),
                exchangeCurrency = configuration.inputCurrency,
                outputCurrency = CurrencyType.Crypto(amount.currency)
            )
        }
        showValue(amount)
    }
}

data class FiatCryptoViewConfiguration(
    val inputCurrency: CurrencyType, // the currency used for input by the user
    val exchangeCurrency: CurrencyType, // the currency used for the exchanged amount
    val outputCurrency: CurrencyType = inputCurrency, // the currency used for the model output
    val predefinedAmount: Money = inputCurrency.zeroValue(),
    val canSwap: Boolean = true
) {
    val inputIsSameAsExchange: Boolean
        get() = inputCurrency == exchangeCurrency

    val swapEnabled: Boolean
        get() = canSwap && inputCurrency != exchangeCurrency
}

private fun CurrencyType.zeroValue(): Money =
    when (this) {
        is CurrencyType.Fiat -> FiatValue.zero(fiatCurrency)
        is CurrencyType.Crypto -> CryptoValue.zero(cryptoCurrency)
    }

private fun CurrencyType.symbol(): String =
    when (this) {
        is CurrencyType.Fiat -> Currency.getInstance(fiatCurrency).getSymbol(Locale.getDefault())
        is CurrencyType.Crypto -> cryptoCurrency.displayTicker
    }

sealed class CurrencyType {
    data class Fiat(val fiatCurrency: String) : CurrencyType()
    data class Crypto(val cryptoCurrency: AssetInfo) : CurrencyType()

    fun isCrypto() = this is Crypto
    fun isFiat() = this is Fiat

    fun isSameType(money: Money) =
        when (this) {
            is Fiat -> money is FiatValue
            is Crypto -> money is CryptoValue
        }
}
