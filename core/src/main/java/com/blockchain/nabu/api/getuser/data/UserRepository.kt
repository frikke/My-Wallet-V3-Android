package com.blockchain.nabu.api.getuser.data

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.getDataOrThrow
import com.blockchain.data.toObservable
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService {
    override fun getUser(): Single<NabuUser> =
        getUserStore
            .stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .toObservable()
            .firstElement()
            .toSingle()

    override fun getUserFlow(refreshStrategy: FreshnessStrategy): Flow<NabuUser> =
        getUserStore.stream(refreshStrategy)
            .getDataOrThrow()

    override fun getUserResourceFlow(refreshStrategy: FreshnessStrategy): Flow<DataResource<NabuUser>> =
        getUserStore.stream(refreshStrategy)
}
