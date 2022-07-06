package com.blockchain.nabu.api.getuser.data.store

import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface GetUserDataSource : FlushableDataSource {
    fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, NabuUser>>
}
