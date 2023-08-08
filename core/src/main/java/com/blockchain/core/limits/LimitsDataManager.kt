package com.blockchain.core.limits

import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.txlimits.data.FeatureLimitResponse
import com.blockchain.api.txlimits.data.FeatureName
import com.blockchain.api.txlimits.data.Limit
import com.blockchain.api.txlimits.data.LimitPeriod
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.math.RoundingMode

interface LimitsDataManager {

    fun getLimits(
        outputCurrency: Currency,
        sourceCurrency: Currency,
        targetCurrency: Currency,
        sourceAccountType: String,
        targetAccountType: String,
        legacyLimits: Single<LegacyLimits>
    ): Single<TxLimits>

    fun getFeatureLimits(): Single<List<FeatureWithLimit>>
}

class LimitsDataManagerImpl(
    private val limitsService: TxLimitsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val assetCatalogue: AssetCatalogue
) : LimitsDataManager {

    override fun getLimits(
        outputCurrency: Currency,
        sourceCurrency: Currency,
        targetCurrency: Currency,
        sourceAccountType: String,
        targetAccountType: String,
        legacyLimits: Single<LegacyLimits>
    ): Single<TxLimits> {
        val legacyLimitsToOutputCurrency = legacyLimits.toOutputCurrency(
            outputCurrency,
            exchangeRatesDataManager
        )

        return Single.zip(
            legacyLimitsToOutputCurrency,
            limitsService.getCrossborderLimits(
                outputCurrency = outputCurrency.networkTicker,
                sourceCurrency = sourceCurrency.networkTicker,
                targetCurrency = targetCurrency.networkTicker,
                sourceAccountType = sourceAccountType,
                targetAccountType = targetAccountType
            )
        ) { legacyLimits, crossborderLimits ->
            val maxLegacyLimit = legacyLimits.max
            val maxCrossborderLimit = crossborderLimits.current?.available?.toMoneyValue()
            val maxLimit = when {
                maxCrossborderLimit != null && maxLegacyLimit != null -> TxLimit.Limited(
                    Money.min(maxLegacyLimit, maxCrossborderLimit)
                )

                maxLegacyLimit != null -> TxLimit.Limited(maxLegacyLimit)
                maxCrossborderLimit != null -> TxLimit.Limited(maxCrossborderLimit)
                else -> TxLimit.Unlimited
            }

            val periodicLimits = listOfNotNull(
                crossborderLimits.current?.daily?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.DAILY,
                        it.effective ?: false
                    )
                },
                crossborderLimits.current?.monthly?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.MONTHLY,
                        it.effective ?: false
                    )
                },
                crossborderLimits.current?.yearly?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.YEARLY,
                        it.effective ?: false
                    )
                }
            )

            val upgradedLimits = listOfNotNull(
                crossborderLimits.suggestedUpgrade?.daily?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.DAILY,
                        false
                    )
                },
                crossborderLimits.suggestedUpgrade?.monthly?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.MONTHLY,
                        false
                    )
                },
                crossborderLimits.suggestedUpgrade?.yearly?.let {
                    TxPeriodicLimit(
                        it.limit.toMoneyValue(),
                        TxLimitPeriod.YEARLY,
                        false
                    )
                }
            )

            TxLimits(
                min = TxLimit.Limited(legacyLimits.min),
                max = maxLimit,
                periodicLimits = periodicLimits,
                suggestedUpgrade = crossborderLimits.suggestedUpgrade?.let {
                    SuggestedUpgrade(
                        type = UpgradeType.Kyc(KycTier.values()[it.requiredTier]),
                        upgradedLimits = upgradedLimits
                    )
                }
            )
        }
    }

    override fun getFeatureLimits(): Single<List<FeatureWithLimit>> =
        limitsService.getFeatureLimits()
            .map { response ->
                response.limits.mapNotNull { it.toFeatureWithLimit(assetCatalogue) }
            }

    private fun Single<LegacyLimits>.toOutputCurrency(
        outputCurrency: Currency,
        exchangeRatesDataManager: ExchangeRatesDataManager
    ): Single<LegacyLimits> {
        return flatMap { legacy ->
            val legacyCurrency = assetCatalogue.fromNetworkTicker(legacy.currency)
            if (legacyCurrency != null && outputCurrency != legacyCurrency) {
                exchangeRatesDataManager.exchangeRateLegacy(outputCurrency, legacyCurrency)
                    .firstOrError()
                    .map { exchangeRate ->
                        object : LegacyLimits {
                            override val min: Money
                                get() = legacy.min.toOutputCryptoCurrency(
                                    outputCurrency,
                                    exchangeRate,
                                    RoundingMode.CEILING
                                )
                            override val max: Money?
                                get() = legacy.max?.toOutputCryptoCurrency(
                                    outputCurrency,
                                    exchangeRate,
                                    RoundingMode.FLOOR
                                )
                        }
                    }
            } else {
                Single.just(legacy)
            }
        }
    }

    private fun Limit.toMoneyValue(): Money =
        assetCatalogue.fromNetworkTicker(currency)?.let {
            Money.fromMinor(it, value.toBigInteger())
        } ?: throw IllegalArgumentException("Unknown Fiat currency")
}

private fun Money.toOutputCryptoCurrency(
    outputAsset: Currency?,
    exchangeRate: ExchangeRate,
    roundingMode: RoundingMode
) = when {
    outputAsset == null ||
        outputAsset.networkTicker == this.currencyCode -> this

    this is FiatValue -> exchangeRate.inverse(roundingMode, outputAsset.precisionDp).convert(this)
    else -> throw IllegalStateException("Conversion cannot be performed.")
}

private fun FeatureLimitResponse.toFeatureWithLimit(assetCatalogue: AssetCatalogue): FeatureWithLimit? {
    val feature = name.toFeature() ?: return null
    val featureLimit = when {
        enabled && limit == null -> FeatureLimit.Unspecified
        enabled && limit?.value == null -> FeatureLimit.Infinite
        enabled && limit?.value != null -> {
            val apiMoney = limit?.value!!
            val currency = assetCatalogue.fromNetworkTicker(apiMoney.currency) ?: return null
            val txPeriod = limit?.period?.toLimitPeriod() ?: return null
            val limit = Money.fromMinor(currency, apiMoney.value.toBigInteger())
            FeatureLimit.Limited(TxPeriodicLimit(limit, txPeriod, true))
        }

        else -> FeatureLimit.Disabled
    }

    return FeatureWithLimit(feature, featureLimit)
}

private fun String.toLimitPeriod(): TxLimitPeriod? =
    LimitPeriod.values().find {
        it.name.equals(this, ignoreCase = true)
    }?.let {
        when (it) {
            LimitPeriod.DAY -> TxLimitPeriod.DAILY
            LimitPeriod.MONTH -> TxLimitPeriod.MONTHLY
            LimitPeriod.YEAR -> TxLimitPeriod.YEARLY
        }
    }

private fun String.toFeature(): Feature? =
    FeatureName.values().find {
        it.name.equals(this, ignoreCase = true)
    }?.let {
        when (it) {
            FeatureName.SEND_CRYPTO -> Feature.SEND_FROM_TRADING_TO_PRIVATE
            FeatureName.RECEIVE_CRYPTO -> Feature.RECEIVE_TO_TRADING
            FeatureName.SWAP_CRYPTO -> Feature.SWAP
            FeatureName.BUY_AND_SELL -> Feature.BUY_SELL
            FeatureName.BUY_WITH_CARD -> Feature.CARD_PURCHASES
            FeatureName.BUY_AND_DEPOSIT_WITH_BANK -> Feature.FIAT_DEPOSIT
            FeatureName.WITHDRAW_WITH_BANK -> Feature.FIAT_WITHDRAWAL
            FeatureName.SAVINGS_INTEREST -> Feature.REWARDS
        }
    }

sealed class TxLimit(private val _amount: Money?) {
    data class Limited(private val value: Money) : TxLimit(value)
    object Unlimited : TxLimit(null)

    val amount: Money
        get() = when (this) {
            is Limited -> _amount ?: throw IllegalStateException("Limited limit must have an amount")
            else -> throw IllegalStateException("Requesting value of an infinitive limit")
        }
}

const val CUSTODIAL_LIMITS_ACCOUNT = "CUSTODIAL"
const val NON_CUSTODIAL_LIMITS_ACCOUNT = "NON_CUSTODIAL"

data class TxLimits(
    val min: TxLimit.Limited,
    val max: TxLimit,
    val periodicLimits: List<TxPeriodicLimit> = emptyList(),
    val suggestedUpgrade: SuggestedUpgrade? = null
) {

    val minAmount: Money
        get() = min.amount

    val maxAmount: Money
        get() = max.amount

    fun isAmountUnderMin(amount: Money) = min.amount > amount

    fun isAmountOverMax(amount: Money) = (max as? TxLimit.Limited)?.let {
        it.amount < amount
    } ?: false

    fun isAmountInRange(amount: Money): Boolean =
        !(isAmountUnderMin(amount) || isAmountOverMax(amount))

    // TODO we need to combine the suggested upgrades also but this requires some refactoring and can wait for now
    fun combineWith(other: TxLimits): TxLimits =
        this.copy(
            min = TxLimit.Limited(Money.max(other.minAmount, minAmount)),
            max = when {
                max is TxLimit.Unlimited && other.max is TxLimit.Unlimited -> TxLimit.Unlimited
                other.max is TxLimit.Unlimited -> max
                max is TxLimit.Unlimited -> other.max
                else -> TxLimit.Limited(Money.min(other.maxAmount, maxAmount))
            }
        )

    companion object {
        fun fromAmounts(min: Money, max: Money) =
            TxLimits(
                min = TxLimit.Limited(min),
                max = TxLimit.Limited(max)
            )

        fun withMinAndUnlimitedMax(min: Money) =
            TxLimits(
                min = TxLimit.Limited(min),
                max = TxLimit.Unlimited
            )
    }
}

enum class TxLimitPeriod {
    DAILY,
    MONTHLY,
    YEARLY
}

data class TxPeriodicLimit(
    val amount: Money,
    val period: TxLimitPeriod,
    val effective: Boolean
)

data class SuggestedUpgrade(
    val type: UpgradeType,
    val upgradedLimits: List<TxPeriodicLimit>
)

sealed class UpgradeType {
    data class Kyc(val proposedTier: KycTier) : UpgradeType()
}

data class FeatureWithLimit(
    val feature: Feature,
    val limit: FeatureLimit
)

enum class Feature {
    SEND_FROM_TRADING_TO_PRIVATE,
    RECEIVE_TO_TRADING,
    SWAP,
    BUY_SELL,
    CARD_PURCHASES,
    FIAT_DEPOSIT,
    FIAT_WITHDRAWAL,
    REWARDS
}

sealed class FeatureLimit {
    object Disabled : FeatureLimit()
    object Unspecified : FeatureLimit()
    object Infinite : FeatureLimit()
    data class Limited(val limit: TxPeriodicLimit) : FeatureLimit()
}
