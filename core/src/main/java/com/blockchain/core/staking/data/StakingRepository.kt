package com.blockchain.core.staking.data

import com.blockchain.api.staking.data.StakingBalanceDto
import com.blockchain.api.staking.data.StakingEligibilityDto
import com.blockchain.core.staking.data.datasources.StakingBalanceStore
import com.blockchain.core.staking.data.datasources.StakingEligibilityStore
import com.blockchain.core.staking.data.datasources.StakingRatesStore
import com.blockchain.core.staking.domain.StakingService
import com.blockchain.core.staking.domain.model.StakingAccountBalance
import com.blockchain.core.staking.domain.model.StakingEligibility
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.mapData
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class StakingRepository(
    private val stakingRatesStore: StakingRatesStore,
    private val stakingEligibilityStore: StakingEligibilityStore,
    private val stakingBalanceStore: StakingBalanceStore,
    private val assetCatalogue: AssetCatalogue,
    private val stakingFeatureFlag: FeatureFlag
) : StakingService {

    // we use the rates endpoint to determine whether the user has access to staking cryptos
    override fun getAvailabilityForAsset(
        currency: Currency,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(currency.networkTicker)
        }

    override suspend fun getRateForAsset(
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

    // TODO(dserrano) - STAKING - ask @Seba how this should be used in coinview
    override suspend fun getEligibilityForAsset(
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
}
