package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent

data class ActivityDetailViewState(
    val activityDetailItems: DataResource<ActivityDetail>
) : ViewState

data class ActivityDetail(
    val itemGroups: List<List<ActivityComponent>>,
    val floatingActions: List<ActivityComponent>
)
