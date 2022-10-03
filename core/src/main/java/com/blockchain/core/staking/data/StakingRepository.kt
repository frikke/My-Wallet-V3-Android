package com.blockchain.core.staking.data

import com.blockchain.core.staking.data.datasources.StakingRatesStore
import com.blockchain.core.staking.domain.model.StakingService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.store.mapData
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class StakingRepository(
    private val stakingRatesStore: StakingRatesStore
) : StakingService {

    override fun getEligibilityForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Boolean>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates.containsKey(ticker)
        }

    override suspend fun getRateForAsset(
        ticker: String,
        refreshStrategy: FreshnessStrategy
    ): Flow<DataResource<Double>> =
        stakingRatesStore.stream(refreshStrategy).mapData {
            it.rates[ticker]?.rate ?: 0.0
        }

    // TODO(dserrano) - STAKING - add StakingBalanceStore checks here
    override fun getActiveAssets(refreshStrategy: FreshnessStrategy): Flow<Set<AssetInfo>> =
        flowOf(setOf(CryptoCurrency.ETHER))
}
