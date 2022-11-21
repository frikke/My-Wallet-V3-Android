package com.blockchain.home.presentation.activity.detail.custodial

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups

data class CustodialActivityDetailModelState(
    val activityDetail: DataResource<ActivityDetailGroups> = DataResource.Loading,
) : ModelState
