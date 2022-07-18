package com.blockchain.nabu.api.getuser.data

import com.blockchain.nabu.api.getuser.domain.UserService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.StoreRequest
import com.blockchain.store.asObservable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal class UserRepository(
    private val getUserStore: GetUserStore
) : UserService {

    private fun getUser(refresh: Boolean): Observable<NabuUser> {
        return getUserStore.stream(StoreRequest.Cached(forceRefresh = refresh))
            .asObservable { it }
    }

    override fun getUser(): Single<NabuUser> =
        getUser(refresh = false).firstElement().toSingle()
}
