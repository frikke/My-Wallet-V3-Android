package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem

data class ActivityDetailViewState(
    val activityDetail: DataResource<ActivityDetail>
) : ViewState

data class ActivityDetail(
    val icon: ActivityIconState,
    val title: String,
    val subtitle: String,
    val detailItems: List<ActivityDetailGroup>,
    val floatingActions: List<ActivityComponent>
)

data class ActivityDetailGroup(
    val title: String?,
    val itemGroup: List<ActivityComponent>
)
