package com.blockchain.core.limits

import com.blockchain.api.services.TxLimitsService
import com.blockchain.api.txlimits.data.ApiMoneyMinor
import com.blockchain.api.txlimits.data.CurrentLimits
import com.blockchain.api.txlimits.data.FeatureLimitResponse
import com.blockchain.api.txlimits.data.FeatureName
import com.blockchain.api.txlimits.data.FeaturePeriodicLimit
import com.blockchain.api.txlimits.data.GetCrossborderLimitsResponse
import com.blockchain.api.txlimits.data.GetFeatureLimitsResponse
import com.blockchain.api.txlimits.data.Limit
import com.blockchain.api.txlimits.data.LimitRange
import com.blockchain.api.txlimits.data.PeriodicLimit
import com.blockchain.api.txlimits.data.SuggestedUpgrade
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.nabu.USD
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.RoundingMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LimitsDataManagerImplTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val limitsService: TxLimitsService = mock()
    private val cryptoToFiatRate: ExchangeRate = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock {
        on { exchangeRateLegacy(OUTPUT_CRYPTO_CURRENCY, USD) }.thenReturn(
            Observable.just(cryptoToFiatRate)
        )
    }
    private val assetCatalogue: AssetCatalogue = mock()

    private val subject = LimitsDataManagerImpl(
        limitsService = limitsService,
        exchangeRatesDataManager = exchangeRatesDataManager,
        assetCatalogue = assetCatalogue
    )

    @Before
    fun setup() {
        whenever(assetCatalogue.fromNetworkTicker(OUTPUT_CRYPTO_CURRENCY.networkTicker))
            .thenReturn(OUTPUT_CRYPTO_CURRENCY)
        whenever(assetCatalogue.fromNetworkTicker(OUTPUT_FIAT_CURRENCY.networkTicker))
            .thenReturn(OUTPUT_FIAT_CURRENCY)
    }

    @Test
    fun `given crypto output currency should call legacy api and convert it's limits to output currency`() {
        whenever(limitsService.getCrossborderLimits(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(NO_CROSSBORDER_LIMITS))

        val legacyMinLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 1123L.toBigInteger())
        val convertedMinLimit = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, 2000.0.toBigDecimal())
        val legacyMaxLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 4123L.toBigInteger())
        val convertedMaxLimit = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, 8000.0.toBigDecimal())

        val fiatToCryptoRateCeiling: ExchangeRate = mock {
            on { convert(legacyMinLimit) }.thenReturn(convertedMinLimit)
        }
        val fiatToCryptoRateFloor: ExchangeRate = mock {
            on { convert(legacyMaxLimit) }.thenReturn(convertedMaxLimit)
        }
        whenever(cryptoToFiatRate.inverse(RoundingMode.CEILING, OUTPUT_CRYPTO_CURRENCY.precisionDp))
            .thenReturn(fiatToCryptoRateCeiling)
        whenever(cryptoToFiatRate.inverse(RoundingMode.FLOOR, OUTPUT_CRYPTO_CURRENCY.precisionDp))
            .thenReturn(fiatToCryptoRateFloor)

        val legacyLimits = object : LegacyLimits {
            override val min: Money = legacyMinLimit
            override val max: Money = legacyMaxLimit
        }
        val legacyLimitsSingle: Single<LegacyLimits> = Single.just(legacyLimits)

        // swap btc nc -> eth nc
        subject.getLimits(
            outputCurrency = OUTPUT_CRYPTO_CURRENCY,
            sourceCurrency = OUTPUT_CRYPTO_CURRENCY,
            targetCurrency = CryptoCurrency.ETHER,
            sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
            legacyLimits = legacyLimitsSingle
        ).test()
            .assertComplete()
            .assertValue { limits ->
                limits.min.amount == convertedMinLimit &&
                    limits.max is TxLimit.Limited &&
                    limits.max.amount == convertedMaxLimit
            }

        legacyLimitsSingle.test().assertComplete()
        verify(exchangeRatesDataManager).exchangeRateLegacy(OUTPUT_CRYPTO_CURRENCY, OUTPUT_FIAT_CURRENCY)
        verify(cryptoToFiatRate).inverse(RoundingMode.CEILING, OUTPUT_CRYPTO_CURRENCY.precisionDp)
        verify(cryptoToFiatRate).inverse(RoundingMode.FLOOR, OUTPUT_CRYPTO_CURRENCY.precisionDp)
    }

    @Test
    fun `given fiat output currency should not convert legacy api limits`() {
        whenever(limitsService.getCrossborderLimits(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(NO_CROSSBORDER_LIMITS))

        val legacyMinLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 1123L.toBigInteger())
        val legacyMaxLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 4123L.toBigInteger())
        val legacyLimits = object : LegacyLimits {
            override val min: Money = legacyMinLimit
            override val max: Money = legacyMaxLimit
        }
        val legacyLimitsSingle: Single<LegacyLimits> = Single.just(legacyLimits)

        subject.getLimits(
            outputCurrency = OUTPUT_FIAT_CURRENCY,
            sourceCurrency = OUTPUT_FIAT_CURRENCY,
            targetCurrency = CryptoCurrency.ETHER,
            sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
            legacyLimits = legacyLimitsSingle
        ).test()
            .assertComplete()
            .assertValue { limits ->
                limits.min.amount == legacyMinLimit &&
                    limits.max.amount == legacyMaxLimit
            }

        legacyLimitsSingle.test().assertComplete()
        verifyNoMoreInteractions(exchangeRatesDataManager)
    }

    @Test
    fun `should fetch crossborder limits and correctly construct TxLimits`() {
        val crossborderLimitsResponse = createFakeCrossborderLimits(OUTPUT_CRYPTO_CURRENCY.networkTicker)
        val crossborderMaxLimit = CryptoValue.fromMinor(
            OUTPUT_CRYPTO_CURRENCY,
            AVAILABLE_LIMIT.toBigInteger()
        )
        whenever(limitsService.getCrossborderLimits(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(crossborderLimitsResponse))

        val legacyMinLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 1123L.toBigInteger())
        val convertedMinLimit = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, 2000.0.toBigDecimal())
        val legacyMaxLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 4123L.toBigInteger())
        val convertedMaxLimit = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, 8000.0.toBigDecimal())

        val fiatToCryptoRateCeiling: ExchangeRate = mock {
            on { convert(legacyMinLimit) }.thenReturn(convertedMinLimit)
        }
        val fiatToCryptoRateFloor: ExchangeRate = mock {
            on { convert(legacyMaxLimit) }.thenReturn(convertedMaxLimit)
        }
        whenever(cryptoToFiatRate.inverse(RoundingMode.CEILING, OUTPUT_CRYPTO_CURRENCY.precisionDp))
            .thenReturn(fiatToCryptoRateCeiling)
        whenever(cryptoToFiatRate.inverse(RoundingMode.FLOOR, OUTPUT_CRYPTO_CURRENCY.precisionDp))
            .thenReturn(fiatToCryptoRateFloor)

        val legacyLimits = object : LegacyLimits {
            override val min: Money = legacyMinLimit
            override val max: Money = legacyMaxLimit
        }
        val legacyLimitsSingle: Single<LegacyLimits> = Single.just(legacyLimits)

        subject.getLimits(
            outputCurrency = OUTPUT_CRYPTO_CURRENCY,
            sourceCurrency = OUTPUT_CRYPTO_CURRENCY,
            targetCurrency = CryptoCurrency.ETHER,
            sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
            legacyLimits = legacyLimitsSingle
        ).test()
            .assertComplete()
            .assertValue { limits ->
                limits.min.amount == convertedMinLimit &&
                    limits.max is TxLimit.Limited &&
                    limits.max.amount == Money.min(convertedMaxLimit, crossborderMaxLimit) &&
                    limits.periodicLimits.containsAll(
                        listOf(
                            TxPeriodicLimit(
                                amount = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, DAILY_LIMIT.toBigInteger()),
                                period = TxLimitPeriod.DAILY,
                                effective = true
                            ),
                            TxPeriodicLimit(
                                amount = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, MONTHLY_LIMIT.toBigInteger()),
                                period = TxLimitPeriod.MONTHLY,
                                effective = false
                            ),
                            TxPeriodicLimit(
                                amount = CryptoValue.fromMinor(OUTPUT_CRYPTO_CURRENCY, YEARLY_LIMIT.toBigInteger()),
                                period = TxLimitPeriod.YEARLY,
                                effective = false
                            )
                        )
                    ) &&
                    limits.suggestedUpgrade!!.type == UpgradeType.Kyc(KycTier.GOLD) &&
                    limits.suggestedUpgrade!!.upgradedLimits.containsAll(
                        listOf(
                            TxPeriodicLimit(
                                amount = CryptoValue.fromMinor(
                                    OUTPUT_CRYPTO_CURRENCY,
                                    SUGGESTED_DAILY_LIMIT.toBigInteger()
                                ),
                                period = TxLimitPeriod.DAILY,
                                effective = false
                            ),
                            TxPeriodicLimit(
                                amount = CryptoValue.fromMinor(
                                    OUTPUT_CRYPTO_CURRENCY,
                                    SUGGESTED_YEARLY_LIMIT.toBigInteger()
                                ),
                                period = TxLimitPeriod.YEARLY,
                                effective = false
                            )
                        )
                    )
            }

        legacyLimitsSingle.test().assertComplete()
        verify(limitsService).getCrossborderLimits(
            outputCurrency = OUTPUT_CRYPTO_CURRENCY.networkTicker,
            sourceCurrency = OUTPUT_CRYPTO_CURRENCY.networkTicker,
            targetCurrency = "ETH",
            sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT
        )
        verify(assetCatalogue, atLeastOnce()).fromNetworkTicker(OUTPUT_CRYPTO_CURRENCY.networkTicker)
        verify(exchangeRatesDataManager).exchangeRateLegacy(OUTPUT_CRYPTO_CURRENCY, OUTPUT_FIAT_CURRENCY)
        verify(cryptoToFiatRate).inverse(RoundingMode.CEILING, OUTPUT_CRYPTO_CURRENCY.precisionDp)
        verify(cryptoToFiatRate).inverse(RoundingMode.FLOOR, OUTPUT_CRYPTO_CURRENCY.precisionDp)
    }

    @Test
    fun `when legacy max limit and crossborder current limits are present, max limit should be the lower of them`() {
        val crossborderLimitsResponse = createFakeCrossborderLimits(OUTPUT_FIAT_CURRENCY.networkTicker)
        val crossborderMaxLimit =
            FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, AVAILABLE_LIMIT.toBigInteger())
        whenever(limitsService.getCrossborderLimits(any(), any(), any(), any(), any()))
            .thenReturn(Single.just(crossborderLimitsResponse))

        val legacyMinLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 1123L.toBigInteger())
        val legacyMaxLimit = FiatValue.fromMinor(OUTPUT_FIAT_CURRENCY, 4123L.toBigInteger())
        val legacyLimits = object : LegacyLimits {
            override val min: Money = legacyMinLimit
            override val max: Money = legacyMaxLimit
        }
        val legacyLimitsSingle: Single<LegacyLimits> = Single.just(legacyLimits)

        subject.getLimits(
            outputCurrency = OUTPUT_FIAT_CURRENCY,
            sourceCurrency = OUTPUT_FIAT_CURRENCY,
            targetCurrency = CryptoCurrency.ETHER,
            sourceAccountType = NON_CUSTODIAL_LIMITS_ACCOUNT,
            targetAccountType = CUSTODIAL_LIMITS_ACCOUNT,
            legacyLimits = legacyLimitsSingle
        ).test()
            .assertComplete()
            .assertValue { limits ->
                limits.min.amount == legacyMinLimit &&
                    limits.max.amount == crossborderMaxLimit
            }

        legacyLimitsSingle.test().assertComplete()
        verify(limitsService).getCrossborderLimits(
            OUTPUT_FIAT_CURRENCY.networkTicker,
            OUTPUT_FIAT_CURRENCY.networkTicker,
            "ETH",
            NON_CUSTODIAL_LIMITS_ACCOUNT,
            CUSTODIAL_LIMITS_ACCOUNT
        )
    }

    @Test
    fun `get feature limits should fetch the feature limits and map result`() {
        val response = GetFeatureLimitsResponse(
            listOf(
                FeatureLimitResponse(FeatureName.SEND_CRYPTO.name, true, null), // Unspecified
                FeatureLimitResponse(FeatureName.RECEIVE_CRYPTO.name, false, null), // Disabled
                FeatureLimitResponse(
                    FeatureName.SWAP_CRYPTO.name,
                    true,
                    FeaturePeriodicLimit(null, "YEAR")
                ), // Infinite
                FeatureLimitResponse(
                    FeatureName.BUY_AND_SELL.name,
                    true,
                    FeaturePeriodicLimit(ApiMoneyMinor("USD", "4000"), "DAY")
                ), // Limited
                FeatureLimitResponse(
                    FeatureName.BUY_WITH_CARD.name,
                    true,
                    FeaturePeriodicLimit(ApiMoneyMinor("USD", "5000"), "MONTH")
                ), // Limited
                FeatureLimitResponse(
                    FeatureName.BUY_AND_DEPOSIT_WITH_BANK.name,
                    true,
                    FeaturePeriodicLimit(ApiMoneyMinor("USD", "6000"), "YEAR")
                ), // Limited
                FeatureLimitResponse(
                    "UNKNOWN",
                    true,
                    FeaturePeriodicLimit(ApiMoneyMinor("USD", "6000"), "YEAR")
                )
            )
        )
        whenever(limitsService.getFeatureLimits()).thenReturn(Single.just(response))

        subject.getFeatureLimits()
            .test()
            .assertComplete()
            .assertValue { limits ->
                limits.containsAll(
                    listOf(
                        FeatureWithLimit(Feature.SEND_FROM_TRADING_TO_PRIVATE, FeatureLimit.Unspecified),
                        FeatureWithLimit(Feature.RECEIVE_TO_TRADING, FeatureLimit.Disabled),
                        FeatureWithLimit(Feature.SWAP, FeatureLimit.Infinite),
                        FeatureWithLimit(
                            Feature.BUY_SELL,
                            FeatureLimit.Limited(
                                TxPeriodicLimit(
                                    amount = FiatValue.fromMinor(USD, 4000L.toBigInteger()),
                                    period = TxLimitPeriod.DAILY,
                                    effective = true
                                )
                            )
                        ),
                        FeatureWithLimit(
                            Feature.CARD_PURCHASES,
                            FeatureLimit.Limited(
                                TxPeriodicLimit(
                                    amount = FiatValue.fromMinor(USD, 5000L.toBigInteger()),
                                    period = TxLimitPeriod.MONTHLY,
                                    effective = true
                                )
                            )
                        ),
                        FeatureWithLimit(
                            Feature.FIAT_DEPOSIT,
                            FeatureLimit.Limited(
                                TxPeriodicLimit(
                                    amount = FiatValue.fromMinor(USD, 6000L.toBigInteger()),
                                    period = TxLimitPeriod.YEARLY,
                                    effective = true
                                )
                            )
                        )
                    )
                ) && limits.none {
                    it.feature == Feature.FIAT_WITHDRAWAL ||
                        it.feature == Feature.REWARDS
                }
            }

        verify(limitsService).getFeatureLimits()
    }

    companion object {
        private val OUTPUT_CRYPTO_CURRENCY = CryptoCurrency.BTC

        private val NO_CROSSBORDER_LIMITS = GetCrossborderLimitsResponse("NOOP", null, null)

        private const val AVAILABLE_LIMIT = "850"
        private const val DAILY_LIMIT = "1000"
        private const val MONTHLY_LIMIT = "5000"
        private const val YEARLY_LIMIT = "10000"
        private const val SUGGESTED_DAILY_LIMIT = "200000"
        private const val SUGGESTED_YEARLY_LIMIT = "1000000"
        private fun createFakeCrossborderLimits(currency: String) = GetCrossborderLimitsResponse(
            currency = currency,
            current = CurrentLimits(
                available = Limit(currency, AVAILABLE_LIMIT),
                daily = PeriodicLimit(Limit(currency, DAILY_LIMIT), true),
                monthly = PeriodicLimit(Limit(currency, MONTHLY_LIMIT)),
                yearly = PeriodicLimit(Limit(currency, YEARLY_LIMIT))
            ),
            suggestedUpgrade = SuggestedUpgrade(
                available = Limit(currency, "200000"),
                daily = LimitRange(
                    limit = Limit(currency, SUGGESTED_DAILY_LIMIT),
                    available = Limit(currency, "199850"),
                    used = Limit(currency, "150")
                ),
                yearly = LimitRange(
                    limit = Limit(currency, SUGGESTED_YEARLY_LIMIT),
                    available = Limit(currency, "999850"),
                    used = Limit(currency, "150")
                ),
                requiredTier = 2,
                requirements = emptyList()
            )
        )
    }
}

private val OUTPUT_FIAT_CURRENCY = FiatCurrency.fromCurrencyCode("USD")
