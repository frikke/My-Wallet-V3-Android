package com.blockchain.home.presentation.activity.custodial.list.mappers

import androidx.annotation.StringRes
import com.blockchain.coincore.CustodialInterestActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.interest.domain.model.InterestState
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.custodial.list.basicSubtitleStyle
import com.blockchain.home.presentation.activity.custodial.list.basicTitleStyle
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.utils.toFormattedDate
import info.blockchain.wallet.multiaddress.TransactionSummary

// todo tint
@StringRes internal fun CustodialInterestActivitySummaryItem.icon(): Int {
    return when (status) {
        InterestState.PENDING,
        InterestState.PROCESSING,
        InterestState.MANUAL_REVIEW -> R.drawable.ic_tx_confirming
        InterestState.FAILED -> R.drawable.ic_close
        else -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> R.drawable.ic_tx_buy
            TransactionSummary.TransactionType.INTEREST_EARNED -> R.drawable.ic_tx_interest
            TransactionSummary.TransactionType.WITHDRAW -> R.drawable.ic_tx_sell
            else -> R.drawable.ic_tx_buy
        }
    }
}

internal fun CustodialInterestActivitySummaryItem.leadingTitle(): ActivityStackView {
    return ActivityStackView.Text(
        value = TextValue.IntResValue(
            value = when (type) {
                TransactionSummary.TransactionType.DEPOSIT -> R.string.tx_title_added
                TransactionSummary.TransactionType.WITHDRAW -> R.string.tx_title_withdrawn
                TransactionSummary.TransactionType.INTEREST_EARNED -> R.string.tx_title_rewards
                else -> R.string.tx_title_transferred
            },
            args = listOf(asset.displayTicker)
        ),
        style = basicTitleStyle
    )
}

internal fun CustodialInterestActivitySummaryItem.leadingSubtitle(): ActivityStackView {
    val color: ActivityTextColorState = when (status) {
        InterestState.COMPLETE,
        InterestState.PENDING,
        InterestState.PROCESSING,
        InterestState.MANUAL_REVIEW,
        InterestState.CLEARED,
        InterestState.UNKNOWN -> ActivityTextColorState.Muted
        InterestState.REJECTED,
        InterestState.REFUNDED -> ActivityTextColorState.Warning
        InterestState.FAILED -> ActivityTextColorState.Error
    }

    return ActivityStackView.Text(
        value = when (status) {
            InterestState.COMPLETE -> TextValue.StringValue(date.toFormattedDate())
            InterestState.PENDING,
            InterestState.PROCESSING,
            InterestState.MANUAL_REVIEW -> TextValue.IntResValue(R.string.activity_state_pending)
            InterestState.FAILED -> TextValue.IntResValue(R.string.activity_state_failed)
            InterestState.CLEARED -> TextValue.IntResValue(R.string.activity_state_cleared)
            InterestState.REFUNDED -> TextValue.IntResValue(R.string.activity_state_refunded)
            InterestState.REJECTED -> TextValue.IntResValue(R.string.activity_state_rejected)
            InterestState.UNKNOWN -> TextValue.IntResValue(R.string.activity_state_unknown)
        },
        style = basicSubtitleStyle.copy(color = color)
    )
}

internal fun CustodialInterestActivitySummaryItem.trailingTitle(): ActivityStackView {
    val color: ActivityTextColorState = when (status) {
        InterestState.COMPLETE -> ActivityTextColorState.Title
        else -> ActivityTextColorState.Muted
    }

    val strikethrough: Boolean = when (status) {
        InterestState.COMPLETE,
        InterestState.PENDING,
        InterestState.PROCESSING,
        InterestState.MANUAL_REVIEW,
        InterestState.CLEARED,
        InterestState.UNKNOWN -> false
        InterestState.REFUNDED,
        InterestState.REJECTED,
        InterestState.FAILED -> true
    }

    return ActivityStackView.Text(
        value = TextValue.StringValue(value.toStringWithSymbol()),
        style = basicTitleStyle.copy(color = color, strikethrough = strikethrough)
    )
}

internal fun CustodialInterestActivitySummaryItem.trailingSubtitle(): ActivityStackView {
//    val strikethrough: Boolean = when (status) {
//        OrderState.FINISHED,
//        OrderState.AWAITING_FUNDS,
//        OrderState.PENDING_CONFIRMATION,
//        OrderState.PENDING_EXECUTION,
//        OrderState.UNINITIALISED,
//        OrderState.INITIALISED,
//        OrderState.UNKNOWN -> false
//        OrderState.CANCELED,
//        OrderState.FAILED -> true
//    }

//    return ActivityStackView.Text(
//        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
//        style = basicSubtitleStyle.copy(strikethrough = strikethrough)
//    )

    return ActivityStackView.Text(
        value = TextValue.StringValue("not implemented"),
        style = basicSubtitleStyle
    )
}
