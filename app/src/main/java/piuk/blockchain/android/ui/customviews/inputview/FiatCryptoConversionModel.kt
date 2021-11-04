package piuk.blockchain.android.ui.customviews.inputview

import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.math.RoundingMode
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

    val exchangeAmount: Observable<ConvertedAmounts> = Observable.combineLatest(
        internalRate,
        outputRate,
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

    private fun getExchangeRate(
        input: CurrencyType,
        output: CurrencyType
    ): Single<ExchangeRate> {
        return when (input) {
            is CurrencyType.Fiat -> when (output) {
                is CurrencyType.Crypto -> exchangeRates.cryptoToFiatRate(
                    fromAsset = output.cryptoCurrency,
                    toFiat = input.fiatCurrency
                ).map {
                    it.inverse(RoundingMode.FLOOR, CryptoValue.DISPLAY_DP)
                }
                is CurrencyType.Fiat -> {
                    exchangeRates.fiatToFiatRate(input.fiatCurrency, output.fiatCurrency)
                }
            }
            is CurrencyType.Crypto -> when (output) {
                is CurrencyType.Crypto -> {
                    check(output.cryptoCurrency == input.cryptoCurrency)
                    exchangeRates.cryptoToSameCryptoRate(input.cryptoCurrency)
                }
                is CurrencyType.Fiat -> {
                    exchangeRates.cryptoToFiatRate(
                        fromAsset = input.cryptoCurrency,
                        toFiat = output.fiatCurrency
                    )
                }
            }
        }.firstOrError()
    }

    fun amountUpdated(amount: Money) {
        inputValue.onNext(amount)
    }

    fun configUpdated(configuration: FiatCryptoViewConfiguration) {
        internalRate.onNext(ExchangeRate.InvalidRate)
        outputRate.onNext(ExchangeRate.InvalidRate)
        Single.zip(
            getExchangeRate(configuration.inputCurrency, configuration.exchangeCurrency),
            getExchangeRate(configuration.inputCurrency, configuration.outputCurrency)
        ) { internal, output ->
            internalRate.onNext(internal)
            outputRate.onNext(output)
        }.emptySubscribe()
    }

    fun convert(amount: Money, config: FiatCryptoViewConfiguration): Single<Money> {
        val currency = when (amount) {
            is FiatValue -> CurrencyType.Fiat(amount.currencyCode)
            is CryptoValue -> CurrencyType.Crypto(amount.currency)
            else -> throw IllegalStateException("Not supported currency")
        }

        return when (currency) {
            config.inputCurrency -> {
                Single.just(amount)
            }
            config.outputCurrency -> {
                outputRate.map { it.inverse().convert(amount) }
                    .firstOrError()
            }
            config.exchangeCurrency -> {
                internalRate.map { it.inverse().convert(amount) }
                    .firstOrError()
            }
            else -> {
                throw IllegalStateException(
                    "Provided amount should be in one of the following:" +
                        "${config.inputCurrency} " +
                        "or ${config.outputCurrency} " +
                        "or ${config.exchangeCurrency}"
                )
            }
        }
    }
}

private fun ExchangeRatesDataManager.cryptoToSameCryptoRate(
    asset: AssetInfo
): Observable<ExchangeRate> =
    Observable.just(
        ExchangeRate.CryptoToCrypto(
            from = asset,
            to = asset,
            rate = 1.toBigDecimal()
        )
    )
