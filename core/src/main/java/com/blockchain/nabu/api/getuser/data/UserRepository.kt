package com.blockchain.nabu.api.getuser.data

import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.StoreRequest
import com.blockchain.store.asObservable
import com.blockchain.store.getDataOrThrow
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService {


    private fun getUser(refresh: Boolean): Observable<NabuUser> {
        return getUserStore.stream(StoreRequest.Cached(forceRefresh = refresh))
            .asObservable { it }
    }

    override fun getUser(): Single<NabuUser> =
        getUser(refresh = false).firstElement().toSingle()


    private fun getUser(request: StoreRequest): Flow<NabuUser> {
        return getUserStore.stream(request)
            .getDataOrThrow()
    }

    override fun getUserFlow(request: StoreRequest): Flow<NabuUser> =
        getUser(request)
}
