package com.blockchain.home.presentation.activity.detail.custodial

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue

data class CustodialActivityDetail(
    val activity: ActivitySummaryItem,
    val extras: List<CustodialActivityDetailExtra>
)

data class CustodialActivityDetailExtra(
    val title: TextValue,
    val value: TextValue
)
