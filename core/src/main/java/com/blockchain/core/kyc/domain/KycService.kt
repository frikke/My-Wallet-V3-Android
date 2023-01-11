package com.blockchain.core.kyc.domain

import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface KycService {
    // deprecated singles
    @Deprecated("prefer reactive so try to use flow function")
    fun getTiersLegacy(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<KycTiers>

    @Deprecated("prefer reactive so try to use flow function")
    fun getHighestApprovedTierLevelLegacy(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<KycTier>

    suspend fun shouldLaunchProve(): Outcome<Exception, Boolean>

    fun isPendingFor(
        tierLevel: KycTier,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    fun isRejected(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    fun isInProgress(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    fun isRejectedFor(
        tierLevel: KycTier,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    fun isResubmissionRequired(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    fun shouldResubmitAfterRecovery(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Single<Boolean>

    // flow
    fun getTiers(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<KycTiers>>

    fun getHighestApprovedTierLevel(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<KycTier>>
}
