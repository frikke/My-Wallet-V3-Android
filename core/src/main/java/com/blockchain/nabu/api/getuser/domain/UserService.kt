package com.blockchain.nabu.api.getuser.domain

import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.StoreRequest
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface UserService {
    fun getUser(): Single<NabuUser>

    fun getUserFlow(
        request: StoreRequest = StoreRequest.Cached(forceRefresh = true)
    ): Flow<NabuUser>
}
