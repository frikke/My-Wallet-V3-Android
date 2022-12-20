package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource

data class ActivityDetailModelState<ACTIVITY_MODEL>(
    val activityDetail: DataResource<ACTIVITY_MODEL> = DataResource.Loading,
) : ModelState
