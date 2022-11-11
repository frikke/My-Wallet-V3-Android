package com.blockchain.home.presentation.activity.common

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.home.presentation.R
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType

// todo tint
private fun ActivitySummaryItem.icon() = when (this) {
    is CustodialTradingActivitySummaryItem -> {
        when (status) {
            OrderState.FINISHED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
            OrderState.AWAITING_FUNDS,
            OrderState.PENDING_CONFIRMATION,
            OrderState.PENDING_EXECUTION -> R.drawable.ic_tx_confirming
            OrderState.UNINITIALISED, // should not see these next ones ATM
            OrderState.INITIALISED,
            OrderState.UNKNOWN,
            OrderState.CANCELED,
            OrderState.FAILED -> if (type == OrderType.BUY) R.drawable.ic_tx_buy else R.drawable.ic_tx_sell
        }
    }

    else -> {
        R.drawable.ic_tx_confirming
    }
}

fun ActivitySummaryItem.toActivityComponent(): ActivityComponent {
    val leading = listOf<ActivityStackView>()
    val trailing = listOf<ActivityStackView>()

    return ActivityComponent.StackView(
        leadingImage = ActivityIconState.SingleIcon.Local(icon()),
        leading = leading,
        trailing = trailing
    )
}
