package com.blockchain.home.presentation.activity.detail.custodial.mappers

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.icon
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted

internal const val TX_ID_MAX_LENGTH = 15

private fun ActivitySummaryItem.title(): TextValue {
    return when (this) {
        //        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is RecurringBuyActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is TradeActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is FiatActivitySummaryItem -> title()
        else -> TextValue.StringValue("not implemented")
    }
}

private fun CustodialActivityDetail.detailItems(): List<ActivityDetailGroup> {
    return when (activity) {
        //        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is RecurringBuyActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is TradeActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is FiatActivitySummaryItem -> activity.detailItems(extras)
        else -> listOf()
    }
}

fun CustodialActivityDetail.toActivityDetail(): ActivityDetail {
    return ActivityDetail(
        icon = ActivityIconState.SingleIcon.Local(activity.icon()),
        title = activity.title(),
        subtitle = TextValue.StringValue(""),
        detailItems = detailItems(),
        floatingActions = listOf()
    )
}

/**
 * map extras to ActivityStackView
 */
fun CustodialActivityDetailExtra.toActivityComponent() = ActivityComponent.StackView(
    id = this.toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = title,
            style = basicTitleStyle.muted()
        )
    ),
    trailing = listOf(
        ActivityStackView.Text(
            value = value,
            style = basicTitleStyle
        )
    )
)