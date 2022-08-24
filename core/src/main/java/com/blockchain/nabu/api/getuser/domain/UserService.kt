package com.blockchain.nabu.api.getuser.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Feature
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface UserService {
    // todo (othman) refactor later as Single is used in many places
    fun getUser(): Single<NabuUser>

    fun getUserFlow(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = true)
    ): Flow<NabuUser>

    fun isEligibleFor(feature: Feature): Flow<DataResource<Boolean>>
}
