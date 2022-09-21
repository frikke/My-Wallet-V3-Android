package com.blockchain.core.kyc.domain

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface KycService {
    // deprecated singles
    @Deprecated("prefer reactive so try to use flow function")
    fun getTiersLegacy(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<KycTiers>

    @Deprecated("prefer reactive so try to use flow function")
    fun getHighestApprovedTierLevelLegacy(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<KycTier>

    fun isPendingFor(
        tierLevel: KycTier,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    fun isRejected(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    fun isInProgress(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    fun isRejectedFor(
        tierLevel: KycTier,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    fun isResubmissionRequired(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    fun shouldResubmitAfterRecovery(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<Boolean>

    // flow
    fun getTiers(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<KycTiers>>

    fun getHighestApprovedTierLevel(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<DataResource<KycTier>>
}
