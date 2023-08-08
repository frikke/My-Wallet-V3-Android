package com.blockchain.home.presentation.activity.list.custodial.mappers

import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.image.LocalLogo
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.utils.toFormattedDate

internal fun FiatActivitySummaryItem.iconSummary(): LocalLogo {
    return when (type) {
        TransactionType.DEPOSIT -> LocalLogo.Buy
        TransactionType.WITHDRAWAL -> LocalLogo.Sell
    }
}

internal fun FiatActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionType.DEPOSIT -> com.blockchain.stringResources.R.string.tx_title_deposited
                TransactionType.WITHDRAWAL -> com.blockchain.stringResources.R.string.tx_title_withdrawn
            },
            args = listOf(account.currency.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun FiatActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        TransactionState.FAILED -> ActivityTextColor.Error
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = when (state) {
            TransactionState.COMPLETED,
            TransactionState.MANUAL_REVIEW,
            TransactionState.PENDING -> TextValue.StringValue(date.toFormattedDate())
            TransactionState.FAILED -> TextValue.IntResValue(
                com.blockchain.stringResources.R.string.activity_state_failed
            )
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

private fun FiatActivitySummaryItem.trailingStrikethrough() = when (state) {
    TransactionState.FAILED -> true
    else -> false
}

internal fun FiatActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColor = when (state) {
        TransactionState.COMPLETED -> ActivityTextColor.Title
        else -> ActivityTextColor.Muted
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(fiat.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = trailingStrikethrough())
    )
}

internal fun FiatActivitySummaryItem.trailingSubtitle(): ActivityStackView? {
    return if (value != fiat) {
        return ActivityStackView.Text(
            value = TextValue.StringValue(value.toStringWithSymbol()),
            style = basicSubtitleStyle.copy(strikethrough = trailingStrikethrough())
        )
    } else {
        null
    }
}
