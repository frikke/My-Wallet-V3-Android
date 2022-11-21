package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState

internal val basicTitleStyle = ActivityTextStyleState(
    typography = ActivityTextTypographyState.Paragraph2,
    color = ActivityTextColorState.Title,
    strikethrough = false
)

internal val basicSubtitleStyle = ActivityTextStyleState(
    typography = ActivityTextTypographyState.Caption1,
    color = ActivityTextColorState.Muted,
    strikethrough = false
)

internal fun ActivitySummaryItem.icon() = when (this) {
    is CustodialTradingActivitySummaryItem -> icon()
    is CustodialTransferActivitySummaryItem -> icon()
    is CustodialInterestActivitySummaryItem -> icon()
    is RecurringBuyActivitySummaryItem -> icon()
    is TradeActivitySummaryItem -> icon()
    is FiatActivitySummaryItem -> icon()
    else -> error("${this::class.simpleName} not supported")
}

private fun ActivitySummaryItem.leading(): List<ActivityStackView> {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is RecurringBuyActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is TradeActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is FiatActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        else -> error("${this::class.simpleName} not supported")
    }
}

private fun ActivitySummaryItem.trailing(): List<ActivityStackView> {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is CustodialTransferActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is CustodialInterestActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is RecurringBuyActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is TradeActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is FiatActivitySummaryItem -> listOfNotNull(trailingTitle(), trailingSubtitle())
        else -> error("${this::class.simpleName} not supported")
    }
}

fun ActivitySummaryItem.toActivityComponent(): ActivityComponent {
    return ActivityComponent.StackView(
        id = txId,
        leadingImage = ActivityIconState.SingleIcon.Local(icon()),
        leading = leading(),
        trailing = trailing()
    )
}
