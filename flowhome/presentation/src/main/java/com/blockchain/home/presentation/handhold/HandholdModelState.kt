package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.handhold.HandholdStepStatus

data class HandholdModelState(
    val data: DataResource<List<HandholdStepStatus>> = DataResource.Loading,
    val lastFreshDataTime: Long = 0
) : ModelState