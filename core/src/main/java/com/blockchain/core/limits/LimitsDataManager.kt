package com.blockchain.core.limits

import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.txlimits.data.Limit
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.Tier
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.math.RoundingMode

interface LegacyLimits {
    val min: Money
    val max: Money?
    val currency: String
        get() = min.currencyCode
}

interface LimitsDataManager {

    fun getLimits(
        outputCurrency: String,
        sourceCurrency: String,
        targetCurrency: String,
        sourceAccountType: AssetCategory,
        targetAccountType: AssetCategory,
        legacyLimits: Single<LegacyLimits>
    ): Single<TxLimits>
}

class LimitsDataManagerImpl(
    private val internalFeatureFlagApi: InternalFeatureFlagApi,
    private val limitsService: TxLimitsService,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val assetCatalogue: AssetCatalogue,
    private val authenticator: Authenticator
) : LimitsDataManager {

    override fun getLimits(
        outputCurrency: String,
        sourceCurrency: String,
        targetCurrency: String,
        sourceAccountType: AssetCategory,
        targetAccountType: AssetCategory,
        legacyLimits: Single<LegacyLimits>
    ): Single<TxLimits> = authenticator.authenticate { token ->
        val outputAsset = assetCatalogue.fromNetworkTicker(outputCurrency)

        val legacyLimitsToOutputCurrency = legacyLimits.toOutputCurrency(
            outputAsset, exchangeRatesDataManager
        )

        if (internalFeatureFlagApi.isFeatureEnabled(GatedFeature.SEAMLESS_LIMITS)) {
            Single.zip(
                legacyLimitsToOutputCurrency,
                limitsService.getSeamlessLimits(
                    authHeader = token.authHeader,
                    outputCurrency = outputCurrency,
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                    sourceAccountType = sourceAccountType.name,
                    targetAccountType = targetAccountType.name
                )
            ) { legacyLimits, seamlessLimits ->
                val maxLegacyLimit = legacyLimits.max
                val maxSeamlessLimit = seamlessLimits.current?.available?.toMoneyValue()
                val maxLimit = when {
                    maxSeamlessLimit != null && maxLegacyLimit != null -> TxLimit.Limited(
                        Money.min(maxLegacyLimit, maxSeamlessLimit)
                    )
                    maxLegacyLimit != null -> TxLimit.Limited(maxLegacyLimit)
                    maxSeamlessLimit != null -> TxLimit.Limited(maxSeamlessLimit)
                    else -> TxLimit.Unlimited
                }

                val periodicLimits = listOfNotNull(
                    seamlessLimits.current?.daily?.let {
                        TxPeriodicLimit(
                            it.limit.toMoneyValue(),
                            TxLimitPeriod.DAILY,
                            it.effective ?: false
                        )
                    },
                    seamlessLimits.current?.monthly?.let {
                        TxPeriodicLimit(
                            it.limit.toMoneyValue(),
                            TxLimitPeriod.MONTHLY,
                            it.effective ?: false
                        )
                    },
                    seamlessLimits.current?.yearly?.let {
                        TxPeriodicLimit(
                            it.limit.toMoneyValue(),
                            TxLimitPeriod.YEARLY,
                            it.effective ?: false
                        )
                    }
                )

                val upgradedLimits = listOfNotNull(
                    seamlessLimits.suggestedUpgrade?.daily?.let {
                        TxPeriodicLimit(
                            it.limit.toMoneyValue(),
                            TxLimitPeriod.DAILY,
                            false
                        )
                    },
                    seamlessLimits.suggestedUpgrade?.monthly?.let {
                        TxPeriodicLimit(
                            it.limit.toMoneyValue(),
                            TxLimitPeriod.MONTHLY,
                            false
                        )
                    },
                    seamlessLimits.suggestedUpgrade?.yearly?.let {
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
                    suggestedUpgrade = seamlessLimits.suggestedUpgrade?.let {
                        SuggestedUpgrade(
                            type = UpgradeType.Kyc(Tier.values()[it.requiredTier]),
                            upgradedLimits = upgradedLimits
                        )
                    }
                )
            }
        } else {
            legacyLimitsToOutputCurrency
                .map { productLimits ->
                    TxLimits(
                        min = TxLimit.Limited(productLimits.min),
                        max = productLimits.max?.let {
                            TxLimit.Limited(it)
                        } ?: TxLimit.Unlimited,
                        periodicLimits = emptyList(),
                        suggestedUpgrade = null
                    )
                }
        }
    }

    private fun Single<LegacyLimits>.toOutputCurrency(
        outputAsset: AssetInfo?,
        exchangeRatesDataManager: ExchangeRatesDataManager
    ): Single<LegacyLimits> {
        return flatMap { legacy ->
            if (outputAsset != null) {
                exchangeRatesDataManager.cryptoToFiatRate(outputAsset, legacy.currency)
                    .firstOrError()
                    .map { exchangeRate ->
                        object : LegacyLimits {
                            override val min: Money
                                get() = legacy.min.toOutputCryptoCurrency(
                                    outputAsset, exchangeRate, RoundingMode.CEILING
                                )
                            override val max: Money?
                                get() = legacy.max?.toOutputCryptoCurrency(
                                    outputAsset, exchangeRate, RoundingMode.FLOOR
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
            CryptoValue.fromMinor(it, value.toBigInteger())
        } ?: FiatValue.fromMinor(currency, value.toLong())
}

private fun Money.toOutputCryptoCurrency(
    outputAsset: AssetInfo?,
    exchangeRate: ExchangeRate,
    roundingMode: RoundingMode
) = when {
    outputAsset == null ||
        outputAsset.networkTicker == this.currencyCode -> this
    this is FiatValue -> exchangeRate.inverse(roundingMode, outputAsset.precisionDp).convert(this)
    else -> throw IllegalStateException("Conversion cannot be performed.")
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

    fun isMinViolatedByAmount(amount: Money) = min.amount > amount

    fun isMaxViolatedByAmount(amount: Money) = (max as? TxLimit.Limited)?.let {
        it.amount < amount
    } ?: false

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
    data class Kyc(val proposedTier: Tier) : UpgradeType()
}
