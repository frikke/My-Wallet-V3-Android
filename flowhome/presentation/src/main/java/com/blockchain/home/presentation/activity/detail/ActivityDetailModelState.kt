package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups

data class ActivityDetailModelState(
    val activityDetail: DataResource<ActivityDetailGroups> = DataResource.Loading,
) : ModelState
