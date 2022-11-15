package com.blockchain.home.presentation.activity.custodial.list

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.custodial.list.mappers.icon
import com.blockchain.home.presentation.activity.custodial.list.mappers.leadingSubtitle
import com.blockchain.home.presentation.activity.custodial.list.mappers.leadingTitle
import com.blockchain.home.presentation.activity.custodial.list.mappers.trailingSubtitle
import com.blockchain.home.presentation.activity.custodial.list.mappers.trailingTitle

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

private fun ActivitySummaryItem.icon() = when (this) {
    is CustodialTradingActivitySummaryItem -> icon()
    is CustodialTransferActivitySummaryItem -> icon()
    is CustodialInterestActivitySummaryItem -> icon()
    is RecurringBuyActivitySummaryItem -> icon()
    is TradeActivitySummaryItem -> icon()
    else -> {
        R.drawable.ic_tx_confirming
    }
}

private fun ActivitySummaryItem.leading(): List<ActivityStackView> {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is RecurringBuyActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is TradeActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())

        else -> listOf(
            ActivityStackView.Text(
                value = TextValue.StringValue("not implemented"),
                style = basicTitleStyle
            )
        )
    }
}

private fun ActivitySummaryItem.trailing(): List<ActivityStackView> {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is CustodialTransferActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is CustodialInterestActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is RecurringBuyActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is TradeActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())

        else -> listOf(
            ActivityStackView.Text(
                value = TextValue.StringValue("not implemented"),
                style = basicTitleStyle
            )
        )
    }
}

fun ActivitySummaryItem.toActivityComponent(): ActivityComponent {
    return ActivityComponent.StackView(
        leadingImage = ActivityIconState.SingleIcon.Local(icon()),
        leading = leading(),
        trailing = trailing()
    )
}
