package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource

data class ActivityDetailModelState(
    val activity: DataResource<ActivityDetail> = DataResource.Loading,
) : ModelState
