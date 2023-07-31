package piuk.blockchain.android.ui.customviews.inputview

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.properties.Delegates
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import piuk.blockchain.android.databinding.EnterFiatCryptoLayoutBinding
import piuk.blockchain.android.util.AfterTextChangedWatcher

class SingleCurrencyInputView(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs),
    KoinComponent {

    private val binding: EnterFiatCryptoLayoutBinding =
        EnterFiatCryptoLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    val onImeAction: Observable<PrefixedOrSuffixedEditText.ImeOptions> by lazy {
        binding.enterAmount.onImeAction
    }

    private val amountSubject: PublishSubject<Money> = PublishSubject.create()
    private val currencyPrefs: CurrencyPrefs by inject()

    val amount: Observable<Money>
        get() = amountSubject

    val isConfigured: Boolean
        get() = configuration != SingleInputViewConfiguration.Undefined

    init {
        with(binding) {
            exchangeAmount.gone()

            enterAmount.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable?) {
                    configuration.let {
                        when (it) {
                            is SingleInputViewConfiguration.Defined -> {
                                val amount = enterAmount.bigDecimalValue?.let { amount ->
                                    Money.fromMajor(it.currency, amount)
                                } ?: Money.zero(it.currency)
                                amountSubject.onNext(amount)
                            }
                            is SingleInputViewConfiguration.Undefined -> {
                            }
                        }
                    }
                }
            })
        }
    }

    var maxLimit by Delegates.observable(
        Money.fromMinor(
            currencyPrefs.selectedFiatCurrency,
            Long.MAX_VALUE.toBigInteger()
        )
    ) { _, oldValue, newValue ->
        if (newValue != oldValue) {
            updateFilters(binding.enterAmount.configuration.prefixOrSuffix)
        }
    }

    private fun updateFilters(prefixOrSuffix: String) {
        val maxDecimalDigitsForAmount = maxLimit.userDecimalPlaces
        val maxIntegerDigitsForAmount = maxLimit.toStringParts().major.length
        binding.enterAmount.addFilter(maxDecimalDigitsForAmount, maxIntegerDigitsForAmount, prefixOrSuffix)
    }

    var configuration by Delegates.observable<SingleInputViewConfiguration>(
        SingleInputViewConfiguration.Undefined
    ) { _, _, newValue ->
        with(binding.enterAmount) {
            filters = emptyArray()
            when (newValue) {
                is SingleInputViewConfiguration.Defined -> {
                    val dTicker = newValue.currency.displayTicker
                    updateFilters(dTicker)
                    configuration = PrefixedOrSuffixedEditText.Configuration(
                        prefixOrSuffix = dTicker,
                        isPrefix = newValue.currency.type == CurrencyType.FIAT,
                        initialText = newValue.predefinedAmount.toStringWithoutSymbol()
                            .replace(
                                DecimalFormatSymbols(java.util.Locale.getDefault()).groupingSeparator.toString(),
                                ""
                            )
                            .removeSuffix("${DecimalFormatSymbols(Locale.getDefault()).decimalSeparator}00")
                    )
                    amountSubject.onNext(
                        newValue.predefinedAmount
                    )
                }
                SingleInputViewConfiguration.Undefined -> {
                }
            }
        }
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
}

sealed class SingleInputViewConfiguration {
    object Undefined : SingleInputViewConfiguration()
    data class Defined(
        val currency: Currency,
        val predefinedAmount: Money = Money.zero(currency)
    ) : SingleInputViewConfiguration()
}
