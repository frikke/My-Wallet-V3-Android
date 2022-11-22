package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted

internal const val TX_ID_MAX_LENGTH = 15

@DrawableRes internal fun ActivitySummaryItem.iconDetail() = when (this) {
//    is CustodialTradingActivitySummaryItem -> iconSummary()
//    is CustodialTransferActivitySummaryItem -> iconSummary()
//    is CustodialInterestActivitySummaryItem -> iconSummary()
//    is RecurringBuyActivitySummaryItem -> iconSummary()
//    is TradeActivitySummaryItem -> iconSummary()
    is FiatActivitySummaryItem -> iconDetail()
    else -> /*error("${this::class.simpleName} not supported")*/ R.drawable.ic_filter // todo temp
}

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
        else -> emptyList()
    }
}

private fun CustodialActivityDetail.floatingActions(): List<ActivityComponent> {
    return when (activity) {
        //        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is RecurringBuyActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        //        is TradeActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is FiatActivitySummaryItem -> emptyList()
        else -> emptyList()
    }
}

fun CustodialActivityDetail.toActivityDetail(): ActivityDetail {
    return ActivityDetail(
        icon = ActivityIconState.SingleIcon.Local(activity.iconDetail()),
        title = activity.title(),
        subtitle = TextValue.StringValue(""), // todo
        detailItems = detailItems(),
        floatingActions = floatingActions()
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
