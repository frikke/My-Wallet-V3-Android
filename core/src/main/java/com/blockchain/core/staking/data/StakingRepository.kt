package com.blockchain.core.staking.data

import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.staking.data.datasources.StakingBalanceStore
import com.blockchain.core.staking.data.datasources.StakingEligibilityStore
import com.blockchain.core.staking.data.datasources.StakingLimitsStore
import com.blockchain.core.staking.data.datasources.StakingRatesStore
import com.blockchain.core.staking.domain.StakingActivity
import com.blockchain.core.staking.domain.StakingActivityAttributes
import com.blockchain.core.staking.domain.StakingService
import com.blockchain.core.staking.domain.StakingState
import com.blockchain.core.staking.domain.model.StakingAccountBalance
import com.blockchain.core.staking.domain.model.StakingEligibility
import com.blockchain.core.staking.domain.model.StakingLimits
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.common.extensions.toTransactionType
import com.blockchain.nabu.models.responses.simplebuy.TransactionAttributesResponse
import com.blockchain.nabu.models.responses.simplebuy.TransactionResponse
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class StakingRepository(
    private val stakingRatesStore: StakingRatesStore,
    private val stakingEligibilityStore: StakingEligibilityStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val assetCatalogue: AssetCatalogue,
    private val stakingFeatureFlag: FeatureFlag,
    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore,
    private val stakingLimitsStore: StakingLimitsStore,
    private val currencyPrefs: CurrencyPrefs
) : StakingService {

    // we use the rates endpoint to determine whether the user has access to staking cryptos
    override fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(currency.networkTicker)
        }

    override fun getRateForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Double>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates[currency.networkTicker]?.rate ?: 0.0
        }

    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> =
        flow {
            if (stakingFeatureFlag.coEnabled()) {
                emitAll(
                    stakingBalanceStore.stream(refreshStrategy)
                        .getDataOrThrow().map {
                            it.keys.map { assetTicker ->
                                assetCatalogue.fromNetworkTicker(assetTicker) as? AssetInfo
                                    ?: throw IllegalStateException(
                                        "Failed mapping unknown asset $assetTicker for Staking"
                                    )
                            }.toSet()
                        }
                )
            } else {
                emit(emptySet())
            }
        }

    override fun getBalanceForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingAccountBalance>> =
        stakingBalanceStore.stream(refreshStrategy).mapData { assetDataMap ->
            assetDataMap[currency.networkTicker]?.toStakingBalance(currency) ?: StakingAccountBalance.zeroBalance(
                currency
            )
        }

    override fun getEligibilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingEligibility>> {
        return stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            eligibilityMap[currency.networkTicker]?.let { eligibility ->
                if (eligibility.isEligible) {
                    StakingEligibility.Eligible
                } else {
                    eligibility.reason.toIneligibilityReason()
                }
            } ?: StakingEligibility.Ineligible.default()
        }
    }

    override fun getStakingEligibility(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingEligibility>> {
        return stakingEligibilityStore.stream(refreshStrategy).mapData { eligibilityMap ->
            if (eligibilityMap.values.any { it.isEligible }) {
                StakingEligibility.Eligible
            } else {
                StakingEligibility.Ineligible.default()
            }
        }
    }

    override fun getActivity(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<List<StakingActivity>>> =
        paymentTransactionHistoryStore
            .stream(
                refreshStrategy.withKey(
                    PaymentTransactionHistoryStore.Key(product = STAKING_PRODUCT_NAME, type = null)
                )
            )
            .mapData { stakingActivityResponse ->
                stakingActivityResponse.items
                    .filter { transaction ->
                        assetCatalogue.fromNetworkTicker(
                            transaction.amount.symbol
                        )?.networkTicker == currency.networkTicker
                    }.map { transaction ->
                        transaction.toStakingActivity(currency)
                    }
            }

    override fun getLimitsForAllAssets(
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Map<AssetInfo, StakingLimits>>> =
        stakingLimitsStore.stream(refreshStrategy).mapData { stakingLimits ->
            stakingLimits.limits.entries.mapNotNull { (assetTicker, limits) ->
                assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let { asset ->

                    val minDepositFiatValue = Money.fromMinor(
                        currencyPrefs.selectedFiatCurrency,
                        limits.minDepositValue.toBigInteger()
                    )

                    val stakingLimit = StakingLimits(
                        minDepositValue = minDepositFiatValue,
                        bondingDays = limits.bondingDays,
                        unbondingDays = limits.unbondingDays ?: 0,
                        withdrawalsDisabled = limits.disabledWithdrawals ?: false
                    )

                    Pair(asset, stakingLimit)
                }
            }.toMap()
        }

    override fun getLimitsForAsset(
        asset: AssetInfo,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<StakingLimits>> =
        getLimitsForAllAssets(refreshStrategy).mapData { mapAssetWithLimits ->
            mapAssetWithLimits[asset] ?: throw NoSuchElementException("Unable to get limits for ${asset.networkTicker}")
        }

    private fun TransactionResponse.toStakingActivity(currency: Currency): StakingActivity =
        StakingActivity(
            value = CryptoValue.fromMinor(currency as AssetInfo, amountMinor.toBigInteger()),
            id = id,
            insertedAt = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
            state = state.toStakingState(),
            type = type.toTransactionType(),
            extraAttributes = extraAttributes?.toDomain()
        )

    private fun String.toStakingState(): StakingState =
        when (this) {
            TransactionResponse.FAILED -> StakingState.FAILED
            TransactionResponse.REJECTED -> StakingState.REJECTED
            TransactionResponse.PROCESSING -> StakingState.PROCESSING
            TransactionResponse.CREATED,
            TransactionResponse.COMPLETE -> StakingState.COMPLETE
            TransactionResponse.PENDING -> StakingState.PENDING
            TransactionResponse.MANUAL_REVIEW -> StakingState.MANUAL_REVIEW
            TransactionResponse.CLEARED -> StakingState.CLEARED
            TransactionResponse.REFUNDED -> StakingState.REFUNDED
            else -> StakingState.UNKNOWN
        }

    private fun TransactionAttributesResponse.toDomain() = StakingActivityAttributes(
        address = address,
        confirmations = confirmations,
        hash = hash,
        id = id,
        transactionHash = txHash,
        transferType = transferType,
        beneficiary = beneficiary
    )

    private fun StakingBalanceDto.toStakingBalance(currency: Currency): StakingAccountBalance =
        StakingAccountBalance(
            totalBalance = Money.fromMinor(currency, totalBalance.toBigInteger()),
            lockedBalance = Money.fromMinor(currency, lockedBalance.toBigInteger()),
            pendingDeposit = Money.fromMinor(currency, pendingDeposit.toBigInteger()),
            pendingWithdrawal = Money.fromMinor(currency, pendingWithdrawal.toBigInteger())
        )

    private fun String.toIneligibilityReason(): StakingEligibility.Ineligible {
        return when {
            this.isEmpty() -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.DEFAULT_REASON_NONE -> StakingEligibility.Ineligible.NONE
            this == StakingEligibilityDto.UNSUPPORTED_REGION -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.INVALID_ADDRESS -> StakingEligibility.Ineligible.REGION
            this == StakingEligibilityDto.TIER_TOO_LOW -> StakingEligibility.Ineligible.KYC_TIER
            else -> StakingEligibility.Ineligible.OTHER
        }
    }

    companion object {
        private const val STAKING_PRODUCT_NAME = "STAKING"
    }
}
