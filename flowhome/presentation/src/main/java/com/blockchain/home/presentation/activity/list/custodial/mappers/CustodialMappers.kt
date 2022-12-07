package com.blockchain.home.presentation.activity.list.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography

internal val basicTitleStyle = ActivityTextStyle(
    typography = ActivityTextTypography.Paragraph2,
    color = ActivityTextColor.Title,
    strikethrough = false
)

internal val basicSubtitleStyle = ActivityTextStyle(
    typography = ActivityTextTypography.Caption1,
    color = ActivityTextColor.Muted,
    strikethrough = false
)

internal fun ActivityTextStyle.muted() = copy(color = ActivityTextColor.Muted)

@DrawableRes internal fun ActivitySummaryItem.iconSummary() = when (this) {
    is CustodialTradingActivitySummaryItem -> iconSummary()
    is CustodialTransferActivitySummaryItem -> iconSummary()
    is CustodialInterestActivitySummaryItem -> iconSummary()
    is RecurringBuyActivitySummaryItem -> iconSummary()
    is TradeActivitySummaryItem -> iconSummary()
    is FiatActivitySummaryItem -> iconSummary()
    is CustodialStakingActivitySummaryItem -> R.drawable.ic_activity_buy
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
        else -> emptyList()
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
        else -> emptyList()
    }
}

fun ActivitySummaryItem.toActivityComponent(): ActivityComponent {
    return ActivityComponent.StackView(
        id = txId,
        leadingImage = ActivityIconState.SingleIcon.Local(iconSummary()),
        leading = leading(),
        trailing = trailing()
    )
}
