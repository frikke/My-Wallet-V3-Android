package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityIcon

data class ActivityDetailViewState(
    val activityDetailItems: DataResource<ActivityDetail>
) : ViewState

data class ActivityDetail(
    val icon: ActivityIconState,
    val title: String,
    val subtitle: String,
    val itemGroups: List<List<ActivityComponent>>,
    val floatingActions: List<ActivityComponent>
)
