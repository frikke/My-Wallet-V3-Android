package com.blockchain.nabu.api.kyc.data.store

import com.blockchain.data.DataResource
import com.blockchain.nabu.models.responses.nabu.TiersResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface KycDataSource : FlushableDataSource {
    fun stream(refresh: Boolean): Flow<DataResource<TiersResponse>>
}
