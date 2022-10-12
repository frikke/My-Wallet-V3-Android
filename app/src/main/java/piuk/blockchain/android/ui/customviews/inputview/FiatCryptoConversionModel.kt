package piuk.blockchain.android.ui.customviews.inputview

import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

internal data class ConvertedAmounts(
    val inputAmount: Money,
    val exchangeAmount: Money,
    val outputAmount: Money
)

internal class FiatCryptoConversionModel(
    private val exchangeRates: ExchangeRatesDataManager
) {
    private val inputValue: PublishSubject<Money> = PublishSubject.create()
    private val internalRate: BehaviorSubject<ExchangeRate> = BehaviorSubject.create()
    private val outputRate: BehaviorSubject<ExchangeRate> = BehaviorSubject.create()

    private var wasInternalRateOverridden = false

    val exchangeAmount: Observable<ConvertedAmounts> = Observable.combineLatest(
        internalRate.filter { it.price.toBigInteger() != 0.toBigInteger() },
        outputRate.filter { it.price.toBigInteger() != 0.toBigInteger() },
        inputValue
    ) { internalRate, outputRate, inputValue ->
        calculate(internalRate, outputRate, inputValue)
    }.retry()
        .distinctUntilChanged()

    private fun calculate(
        internalRate: ExchangeRate,
        outputRate: ExchangeRate,
        inputAmount: Money
    ): ConvertedAmounts {
        return ConvertedAmounts(
            inputAmount = inputAmount,
            exchangeAmount = internalRate.convert(inputAmount),
            outputAmount = outputRate.convert(inputAmount)
        )
    }

    fun amountUpdated(amount: Money) {
        inputValue.onNext(amount)
    }

    fun configUpdated(configuration: FiatCryptoViewConfiguration) {
        Single.zip(
            getInternalExchangeRate(configuration.inputCurrency, configuration.exchangeCurrency),
            getOutputExchangeRate(configuration.inputCurrency, configuration.outputCurrency)
        ) { internal, output ->
            internalRate.onNext(internal)
            outputRate.onNext(output)
        }.emptySubscribe()
    }

    fun convert(amount: Money, config: FiatCryptoViewConfiguration): Single<Money> {
        return when (amount.currency.networkTicker) {
            config.inputCurrency.networkTicker -> {
                Single.just(amount)
            }
            config.outputCurrency.networkTicker -> {
                outputRate.map { it.inverse().convert(amount) }
                    .firstOrError()
            }
            config.exchangeCurrency.networkTicker -> {
                internalRate.map {
                    if (it.from.networkTicker == amount.currency.networkTicker) {
                        it.convert(amount)
                    } else {
                        it.inverse().convert(amount)
                    }
                }.firstOrError()
            }
            else -> {
                throw IllegalStateException(
                    "Provided amount should be in one of the following:" +
                        "${config.inputCurrency.displayTicker} " +
                        "or ${config.outputCurrency.displayTicker} " +
                        "or ${config.exchangeCurrency.displayTicker}"
                )
            }
        }
    }

    fun overrideInternalExchangeRate(exchangeRate: ExchangeRate) {
        wasInternalRateOverridden = true
        internalRate.onNext(exchangeRate)
    }

    private fun getInternalExchangeRate(
        input: Currency,
        output: Currency
    ): Single<ExchangeRate> {
        return if (wasInternalRateOverridden) {
            val customRate = internalRate.value
            if (customRate.from == input && customRate.to == output)
                Single.just(customRate)
            else Single.just(customRate.inverse())
        } else {
            internalRate.onNext(ExchangeRate.zeroRateExchangeRate(input, output))
            getExchangeRate(input, output)
        }
    }

    private fun getOutputExchangeRate(
        input: Currency,
        output: Currency
    ): Single<ExchangeRate> {
        outputRate.onNext(ExchangeRate.zeroRateExchangeRate(input, output))
        return getExchangeRate(input, output)
    }

    private fun getExchangeRate(
        input: Currency,
        output: Currency
    ): Single<ExchangeRate> {
        return exchangeRates.exchangeRate(
            input, output
        ).firstOrError()
    }
}
