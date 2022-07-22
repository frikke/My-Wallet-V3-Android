package com.blockchain.nabu.api.getuser.domain

import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.refreshstrategy.RefreshStrategy
import com.blockchain.store.StoreRequest
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface UserService {
    // todo (othman) refactor later as Single is used in many places
    fun getUser(): Single<NabuUser>

    fun getUserFlow(
        refreshStrategy: RefreshStrategy = RefreshStrategy.Cached(refresh = true)
    ): Flow<NabuUser>
}
