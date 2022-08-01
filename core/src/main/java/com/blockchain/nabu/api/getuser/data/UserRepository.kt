package com.blockchain.nabu.api.getuser.data

import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.asObservable
import com.blockchain.store.getDataOrThrow
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService {

    private fun getUser(refresh: Boolean): Observable<NabuUser> {
        return getUserStore.stream(FreshnessStrategy.Cached(forceRefresh = refresh))
            .asObservable()
    }

    override fun getUser(): Single<NabuUser> =
        getUser(refresh = false).firstElement().toSingle()

    // flow
    private fun getUser(refreshStrategy: FreshnessStrategy): Flow<NabuUser> {
        return getUserStore.stream(refreshStrategy)
            .getDataOrThrow()
    }

    override fun getUserFlow(refreshStrategy: FreshnessStrategy): Flow<NabuUser> =
        getUser(refreshStrategy)
}
