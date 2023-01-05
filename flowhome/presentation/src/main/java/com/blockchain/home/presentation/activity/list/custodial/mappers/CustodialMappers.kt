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
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.home.presentation.activity.common.ActivityComponent
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
    is CustodialStakingActivitySummaryItem -> iconSummary()
    is RecurringBuyActivitySummaryItem -> iconSummary()
    is TradeActivitySummaryItem -> iconSummary()
    is FiatActivitySummaryItem -> iconSummary()
    else -> error("${this::class.simpleName} not supported")
}

private fun ActivitySummaryItem.leading(): List<ActivityStackView> {
    return when (this) {
        is CustodialTradingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialTransferActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialInterestActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
        is CustodialStakingActivitySummaryItem -> listOf(leadingTitle(), leadingSubtitle())
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
        is CustodialStakingActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is RecurringBuyActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is TradeActivitySummaryItem -> listOf(trailingTitle(), trailingSubtitle())
        is FiatActivitySummaryItem -> listOfNotNull(trailingTitle(), trailingSubtitle())
        else -> error("${this::class.simpleName} not supported")
    }
}

fun ActivitySummaryItem.toActivityComponent(): ActivityComponent {
    return ActivityComponent.StackView(
        // hack for now - when interacting with interest there are 2 activities with the same txid
        // but e.g. one is SEND the other is INTEREST DEPOSIT
        id = "$txId|${this::class}",
        leadingImage = StackedIcon.SingleIcon(ImageResource.Local(iconSummary())),
        leading = leading(),
        trailing = trailing()
    )
}
