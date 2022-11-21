package com.blockchain.home.presentation.activity.detail.custodial

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource

data class CustodialActivityDetailModelState(
    val activityDetail: DataResource<CustodialActivityDetail> = DataResource.Loading
) : ModelState

data class CustodialActivityDetail(
    val activity: ActivitySummaryItem,
    val extras: List<CustodialActivityDetailExtras>
)

data class CustodialActivityDetailExtras(
    val title: TextValue,
    val value: TextValue
)