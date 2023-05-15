package com.blockchain.home.presentation.activity.detail.custodial.mappers

import com.blockchain.coincore.CustodialActiveRewardsActivitySummaryItem
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.earn.domain.models.EarnRewardsState
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

internal fun CustodialActiveRewardsActivitySummaryItem.iconDetail(): ImageResource {
    return when (state) {
        EarnRewardsState.COMPLETE -> when (type) {
            TransactionSummary.TransactionType.DEPOSIT -> Icons.Filled.Plus
            TransactionSummary.TransactionType.INTEREST_EARNED -> Icons.Filled.Rewards
            TransactionSummary.TransactionType.WITHDRAW -> Icons.Filled.Minus
            TransactionSummary.TransactionType.DEBIT -> Icons.Filled.Minus
            else -> Icons.Filled.Plus
        }
        else -> Icons.Filled.Rewards
    }
}

internal fun CustodialActiveRewardsActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = when (type) {
        TransactionSummary.TransactionType.DEPOSIT -> com.blockchain.stringResources.R.string.tx_title_added
        TransactionSummary.TransactionType.WITHDRAW -> com.blockchain.stringResources.R.string.tx_title_withdrawn
        TransactionSummary.TransactionType.INTEREST_EARNED -> com.blockchain.stringResources.R.string.tx_title_rewards
        TransactionSummary.TransactionType.DEBIT -> com.blockchain.stringResources.R.string.tx_title_debited
        else -> com.blockchain.stringResources.R.string.tx_title_transferred
    },
    args = listOf(account.currency.displayTicker)
)

internal fun CustodialActiveRewardsActivitySummaryItem.detailItems(
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
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.amount),
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
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.common_status),
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
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.date),
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
                        value = TextValue.IntResValue(
                            com.blockchain.stringResources.R.string.activity_details_buy_tx_id
                        ),
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
                value = TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_details_copy_tx_id),
                style = ActivityButtonStyle.Tertiary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.Copy,
                    data = txId
                )
            )
        )
    )
)

private fun CustodialActiveRewardsActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (state) {
        EarnRewardsState.COMPLETE,
        EarnRewardsState.REFUNDED,
        EarnRewardsState.UNKNOWN,
        EarnRewardsState.CLEARED -> com.blockchain.stringResources.R.string.activity_details_label_complete
        EarnRewardsState.PROCESSING -> com.blockchain.stringResources.R.string.activity_details_label_pending
        EarnRewardsState.PENDING -> com.blockchain.stringResources.R.string.activity_details_label_processing
        EarnRewardsState.MANUAL_REVIEW -> com.blockchain.stringResources.R.string.activity_details_label_manual_review
        EarnRewardsState.REJECTED,
        EarnRewardsState.FAILED -> com.blockchain.stringResources.R.string.activity_details_label_failed
    }
)

private fun CustodialActiveRewardsActivitySummaryItem.statusStyle(): ActivityTagStyle = when (state) {
    EarnRewardsState.REFUNDED,
    EarnRewardsState.COMPLETE,
    EarnRewardsState.UNKNOWN,
    EarnRewardsState.CLEARED -> ActivityTagStyle.Success
    EarnRewardsState.PROCESSING,
    EarnRewardsState.PENDING,
    EarnRewardsState.MANUAL_REVIEW -> ActivityTagStyle.Info
    EarnRewardsState.REJECTED,
    EarnRewardsState.FAILED -> ActivityTagStyle.Error
}

private fun CustodialActiveRewardsActivitySummaryItem.pendingConfirmations(): TextValue? = when {
    isPending() -> TextValue.IntResValue(
        value = com.blockchain.stringResources.R.string.activity_details_label_confirmations_single_line,
        args = listOf(confirmations.coerceAtLeast(0), account.currency.requiredConfirmations)
    )
    else -> null
}

private fun CustodialActiveRewardsActivitySummaryItem.fromLabel(): ActivityComponent = ActivityComponent.StackView(
    id = toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_details_from),
            style = basicTitleStyle.muted()
        )
    ),
    trailing = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(com.blockchain.stringResources.R.string.common_company_name),
            style = basicTitleStyle
        )
    )
)

private fun CustodialActiveRewardsActivitySummaryItem.toLabel(): ActivityComponent = ActivityComponent.StackView(
    id = toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_details_to),
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

private fun CustodialActiveRewardsActivitySummaryItem.fromToLabels(): List<ActivityComponent> = when (type) {
    TransactionSummary.TransactionType.DEPOSIT -> listOf(toLabel())
    TransactionSummary.TransactionType.WITHDRAW -> listOf(fromLabel())
    TransactionSummary.TransactionType.INTEREST_EARNED -> listOf(fromLabel(), toLabel())
    TransactionSummary.TransactionType.DEBIT -> listOf(fromLabel())
    else -> emptyList()
}

internal fun CustodialActiveRewardsActivitySummaryItem.buildActivityDetail() = CustodialActivityDetail(
    activity = this,
    extras = emptyMap()
)
