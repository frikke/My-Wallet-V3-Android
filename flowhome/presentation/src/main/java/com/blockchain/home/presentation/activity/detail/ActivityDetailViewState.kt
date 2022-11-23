package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState

data class ActivityDetailViewState(
    val activityDetail: DataResource<ActivityDetail>
) : ViewState

data class ActivityDetail(
    val icon: ActivityIconState,
    val title: TextValue,
    val subtitle: TextValue,
    val detailItems: List<ActivityDetailGroup>,
    val floatingActions: List<ActivityComponent>
)

data class ActivityDetailGroup(
    val title: String?,
    val itemGroup: List<ActivityComponent>
)
