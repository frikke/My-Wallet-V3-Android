package com.blockchain.nabu.api.getuser.data

import com.blockchain.nabu.api.getuser.data.store.GetUserDataSource
import com.blockchain.nabu.api.getuser.domain.GetUserStoreService
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.asSingle
import io.reactivex.rxjava3.core.Single

internal class GetUserStoreRepository(
    private val getUserDataSource: GetUserDataSource
) : GetUserStoreService {

    override fun getUser(): Single<NabuUser> {
        return getUserDataSource.stream(refresh = false).asSingle { it }
    }
}
