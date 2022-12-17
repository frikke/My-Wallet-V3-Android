package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.CustodialStakingActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.earn.domain.models.staking.StakingState
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtraKey
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicSubtitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.utils.abbreviate
import com.blockchain.utils.toFormattedString
import info.blockchain.wallet.multiaddress.TransactionSummary

@DrawableRes internal fun CustodialStakingActivitySummaryItem.iconDetail(): Int {
    return when (status) {
        StakingState.COMPLETE -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> R.drawable.ic_activity_buy_dark
            TransactionSummary.TransactionType.INTEREST_EARNED -> R.drawable.ic_activity_rewards_dark
            TransactionSummary.TransactionType.WITHDRAW -> R.drawable.ic_activity_sell_dark
            else -> R.drawable.ic_activity_buy_dark
        }
        else -> R.drawable.ic_activity_rewards_dark
    }
}

internal fun CustodialStakingActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = when (type) {
        TransactionSummary.TransactionType.DEPOSIT -> R.string.tx_title_added
        TransactionSummary.TransactionType.WITHDRAW -> R.string.tx_title_withdrawn
        TransactionSummary.TransactionType.INTEREST_EARNED -> R.string.tx_title_rewards
        else -> R.string.tx_title_transferred
    },
    args = listOf(account.currency.displayTicker)
)

internal fun CustodialStakingActivitySummaryItem.detailItems(
    extras: Map<CustodialActivityDetailExtraKey, CustodialActivityDetailExtra>
): List<ActivityDetailGroup> = listOf(
    // deposit ----€10
    // to/from ---- euro
    ActivityDetailGroup(
        title = null,
        itemGroup = listOf(
            // deposit ----€10
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.amount),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(value.toStringWithSymbol()),
                        style = basicTitleStyle
                    )
                )
            )
        )
    ),
    // status ---- success
    // from ---- Trading Account
    // to ---- 0x49...ba41
    ActivityDetailGroup(
        title = null,
        itemGroup = listOfNotNull(
            // status ---- success
            ActivityComponent.StackView(
                id = toString(),
                leading = listOfNotNull(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.common_status),
                        style = basicTitleStyle.muted()
                    ),
                    pendingConfirmations()?.let { pendingConfirmations: TextValue ->
                        ActivityStackView.Text(
                            value = pendingConfirmations,
                            style = basicSubtitleStyle.muted()
                        )
                    }
                ),
                trailing = listOf(
                    ActivityStackView.Tag(
                        value = statusValue(),
                        style = statusStyle()
                    )
                )
            ),
            // from ---- Trading Account
            // to ---- Trading Account
            *fromToLabels().toTypedArray()
        )
    ),
    // date ---- 11:38 PM on Aug 1, 2022
    // transaction id ---- 5c18ca2d-f337-4e02-bbb2-70289c95e28a
    // copy txid
    ActivityDetailGroup(
        title = null,
        itemGroup = listOf(
            // date ---- 11:38 PM on Aug 1, 2022
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.date),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(date.toFormattedString()),
                        style = basicTitleStyle
                    )
                )
            ),

            // transaction id ---- 5c18ca2d-f337-4e02-bbb2-70289c95e28a
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.activity_details_buy_tx_id),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(txId.abbreviate(MAX_ABBREVIATE_LENGTH)),
                        style = basicTitleStyle
                    )
                )
            ),

            // copy txid
            ActivityComponent.Button(
                id = toString(),
                value = TextValue.IntResValue(R.string.activity_details_copy_tx_id),
                style = ActivityButtonStyle.Tertiary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.Copy,
                    data = txId
                )
            )
        )
    )
)

private fun CustodialStakingActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (status) {
        StakingState.COMPLETE,
        StakingState.REFUNDED,
        StakingState.UNKNOWN,
        StakingState.CLEARED -> R.string.activity_details_label_complete
        StakingState.PROCESSING -> R.string.activity_details_label_pending
        StakingState.PENDING -> R.string.activity_details_label_processing
        StakingState.MANUAL_REVIEW -> R.string.activity_details_label_manual_review
        StakingState.REJECTED,
        StakingState.FAILED -> R.string.activity_details_label_failed
    }
)

private fun CustodialStakingActivitySummaryItem.statusStyle(): ActivityTagStyle = when (status) {
    StakingState.REFUNDED,
    StakingState.COMPLETE,
    StakingState.UNKNOWN,
    StakingState.CLEARED -> ActivityTagStyle.Success
    StakingState.PROCESSING,
    StakingState.PENDING,
    StakingState.MANUAL_REVIEW -> ActivityTagStyle.Info
    StakingState.REJECTED,
    StakingState.FAILED -> ActivityTagStyle.Error
}

private fun CustodialStakingActivitySummaryItem.pendingConfirmations(): TextValue? = when {
    isPending() -> TextValue.IntResValue(
        value = R.string.activity_details_label_confirmations_single_line,
        args = listOf(confirmations.coerceAtLeast(0), account.currency.requiredConfirmations)
    )
    else -> null
}

private fun CustodialStakingActivitySummaryItem.fromLabel(): ActivityComponent = ActivityComponent.StackView(
    id = toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(R.string.activity_details_from),
            style = basicTitleStyle.muted()
        )
    ),
    trailing = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(R.string.common_company_name),
            style = basicTitleStyle
        )
    )
)

private fun CustodialStakingActivitySummaryItem.toLabel(): ActivityComponent = ActivityComponent.StackView(
    id = toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(R.string.activity_details_to),
            style = basicTitleStyle.muted()
        )
    ),
    trailing = listOf(
        ActivityStackView.Text(
            value = TextValue.StringValue("${account.currency.displayTicker} ${account.label}"),
            style = basicTitleStyle
        )
    )
)

private fun CustodialStakingActivitySummaryItem.fromToLabels(): List<ActivityComponent> = when (type) {
    TransactionSummary.TransactionType.DEPOSIT -> listOf(toLabel())
    TransactionSummary.TransactionType.WITHDRAW -> listOf(fromLabel())
    TransactionSummary.TransactionType.INTEREST_EARNED -> listOf(fromLabel(), toLabel())
    else -> emptyList()
}

internal fun CustodialStakingActivitySummaryItem.buildActivityDetail() = CustodialActivityDetail(
    activity = this,
    extras = emptyMap()
)
