package com.blockchain.home.handhold

import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface HandholdService {
    fun handholdTasksStatus(): Flow<DataResource<List<HandholdTasksStatus>>>
}
