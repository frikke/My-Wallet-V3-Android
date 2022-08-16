package com.blockchain.nabu.api.kyc.domain

import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.api.kyc.domain.model.KycTiers
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface KycService {
    @Deprecated("prefer reactive so try to use flow function")
    fun getTiersLegacy(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false)
    ): Single<KycTiers>

    fun getTiers(refreshStrategy: FreshnessStrategy): Flow<KycTiers>

    fun markAsStale()
}
