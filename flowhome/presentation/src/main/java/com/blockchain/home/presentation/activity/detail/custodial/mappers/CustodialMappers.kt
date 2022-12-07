package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
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
import com.blockchain.home.presentation.activity.list.custodial.mappers.isSellingPair
import com.blockchain.home.presentation.activity.list.custodial.mappers.isSwapPair
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted

internal const val MAX_ABBREVIATE_LENGTH = 15
internal const val SIDE_ABBREVIATE_LENGTH = 4

@DrawableRes internal fun ActivitySummaryItem.iconDetail() = when (this) {
    is CustodialTradingActivitySummaryItem -> iconDetail()
    is CustodialTransferActivitySummaryItem -> iconDetail()
    is CustodialInterestActivitySummaryItem -> iconDetail()
    is CustodialStakingActivitySummaryItem -> iconDetail()
    is RecurringBuyActivitySummaryItem -> iconDetail()
    is TradeActivitySummaryItem -> when {
        isSellingPair() -> sellIconDetail()
        isSwapPair() -> swapIconDetail()
        else -> error("not supported")
    }
    is FiatActivitySummaryItem -> iconDetail()
    else -> /*error("${this::class.simpleName} not supported")*/ R.drawable.ic_filter // todo temp
}

private fun ActivitySummaryItem.title(): TextValue {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> title()
        is CustodialTransferActivitySummaryItem -> title()
        is CustodialInterestActivitySummaryItem -> title()
        is CustodialStakingActivitySummaryItem -> title()
        is RecurringBuyActivitySummaryItem -> title()
        is TradeActivitySummaryItem -> when {
            isSellingPair() -> sellTitle()
            isSwapPair() -> swapTitle()
            else -> error("not supported")
        }
        is FiatActivitySummaryItem -> title()
        else -> /*error("${this::class.simpleName} not supported")*/ TextValue.StringValue("not implemented")
    }
}

private fun CustodialActivityDetail.detailItems(): List<ActivityDetailGroup> {
    return when (activity) {
        is CustodialTradingActivitySummaryItem -> activity.detailItems(extras)
        is CustodialTransferActivitySummaryItem -> activity.detailItems(extras)
        is CustodialInterestActivitySummaryItem -> activity.detailItems(extras)
        is CustodialStakingActivitySummaryItem -> activity.detailItems(extras)
        is RecurringBuyActivitySummaryItem -> activity.detailItems(extras)
        is TradeActivitySummaryItem -> when {
            activity.isSellingPair() -> activity.sellDetailItems(extras)
            activity.isSwapPair() -> activity.swapDetailItems(extras)
            else -> error("not supported")
        }
        is FiatActivitySummaryItem -> activity.detailItems(extras)
        else -> /*error("${this::class.simpleName} not supported")*/ emptyList()
    }
}

private fun CustodialActivityDetail.floatingActions(): List<ActivityComponent> {
    return when (activity) {
        is CustodialTradingActivitySummaryItem -> emptyList()
        is CustodialTransferActivitySummaryItem -> emptyList()
        is CustodialInterestActivitySummaryItem -> emptyList()
        is CustodialStakingActivitySummaryItem -> emptyList()
        is RecurringBuyActivitySummaryItem -> emptyList()
        is TradeActivitySummaryItem -> emptyList()
        is FiatActivitySummaryItem -> emptyList()
        else -> /*error("${this::class.simpleName} not supported")*/ emptyList()
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
