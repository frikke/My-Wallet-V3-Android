package com.blockchain.nabu.api.getuser.domain

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface UserService {
    // todo (othman) refactor later as Single is used in many places
    @Deprecated("use flow")
    fun getUser(): Single<NabuUser>

    @Deprecated("prefer non throwable Flow")
    fun getUserFlow(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<NabuUser>

    fun getUserResourceFlow(
        refreshStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<NabuUser>>
}
