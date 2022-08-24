package com.blockchain.nabu.api.getuser.data

import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.asObservable
import com.blockchain.store.getDataOrThrow
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService{
    override fun getUser(): Single<NabuUser> =
        getUserStore
            .stream(FreshnessStrategy.Cached(forceRefresh = false))
            .asObservable()
            .firstElement()
            .toSingle()

    override fun getUserFlow(refreshStrategy: FreshnessStrategy): Flow<NabuUser> =
        getUserStore.stream(refreshStrategy)
            .getDataOrThrow()
}
