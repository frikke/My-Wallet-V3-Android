package com.blockchain.nabu.api.getuser.data

import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.refreshstrategy.RefreshStrategy
import com.blockchain.store.StoreRequest
import com.blockchain.store.asObservable
import com.blockchain.store.getDataOrThrow
import com.blockchain.store.toStoreRequest
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService {

    private fun getUser(refresh: Boolean): Observable<NabuUser> {
        return getUserStore.stream(StoreRequest.Cached(forceRefresh = refresh))
            .asObservable()
    }

    override fun getUser(): Single<NabuUser> =
        getUser(refresh = false).firstElement().toSingle()

    // flow
    private fun getUser(refreshStrategy: RefreshStrategy): Flow<NabuUser> {
        return getUserStore.stream(refreshStrategy.toStoreRequest())
            .getDataOrThrow()
    }

    override fun getUserFlow(refreshStrategy: RefreshStrategy): Flow<NabuUser> =
        getUser(refreshStrategy)
}
