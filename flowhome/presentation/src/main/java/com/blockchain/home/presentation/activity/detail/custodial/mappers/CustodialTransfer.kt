package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.CustodialTransferActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.utils.abbreviate
import com.blockchain.utils.toFormattedString

@DrawableRes internal fun CustodialTransferActivitySummaryItem.iconDetail(): Int {
    return when (type) {
        TransactionType.DEPOSIT -> R.drawable.ic_activity_buy_dark
        TransactionType.WITHDRAWAL -> R.drawable.ic_activity_sell_dark
    }
}

internal fun CustodialTransferActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = when (type) {
        TransactionType.DEPOSIT -> R.string.tx_title_received
        TransactionType.WITHDRAWAL -> R.string.tx_title_sent
    },
    args = listOf(account.currency.displayTicker)
)

internal fun CustodialTransferActivitySummaryItem.detailItems(
    extras: List<CustodialActivityDetailExtra>
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
            ),
            // fee ---- €12
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.activity_details_buy_fee),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(fee.toStringWithSymbol()),
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
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.common_status),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Tag(
                        value = statusValue(),
                        style = statusStyle()
                    )
                )
            ),
            // from ---- Trading Account
            run nullableFrom@{
                ActivityComponent.StackView(
                    id = toString(),
                    leading = listOf(
                        ActivityStackView.Text(
                            value = TextValue.IntResValue(R.string.activity_details_from),
                            style = basicTitleStyle.muted()
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            value = when (type) {
                                TransactionType.DEPOSIT -> {
                                    if (recipientAddress.isNotBlank()) TextValue.StringValue(
                                        recipientAddress.abbreviate(
                                            startLength = SIDE_ABBREVIATE_LENGTH,
                                            endLength = SIDE_ABBREVIATE_LENGTH
                                        )
                                    )
                                    else {
                                        return@nullableFrom null
                                    }
                                }
                                TransactionType.WITHDRAWAL -> {
                                    TextValue.StringValue(account.label)
                                }
                            },
                            style = basicTitleStyle
                        )
                    )
                )
            },
            // to ---- 0x49...ba41
            run nullableTo@{
                ActivityComponent.StackView(
                    id = toString(),
                    leading = listOf(
                        ActivityStackView.Text(
                            value = TextValue.IntResValue(R.string.activity_details_to),
                            style = basicTitleStyle.muted()
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            value = when (type) {
                                TransactionType.DEPOSIT -> {
                                    TextValue.StringValue(account.label)
                                }
                                TransactionType.WITHDRAWAL -> {
                                    if (recipientAddress.isNotBlank()) {
                                        TextValue.StringValue(
                                            recipientAddress.abbreviate(
                                                startLength = SIDE_ABBREVIATE_LENGTH,
                                                endLength = SIDE_ABBREVIATE_LENGTH
                                            )
                                        )
                                    } else {
                                        return@nullableTo null
                                    }
                                }
                            },
                            style = basicTitleStyle
                        )
                    )
                )
            },
            // extra
            // payment method
            *extras.map { it.toActivityComponent() }.toTypedArray()
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

private fun CustodialTransferActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (state) {
        TransactionState.COMPLETED -> R.string.activity_details_completed
        TransactionState.PENDING -> R.string.activity_details_label_confirming
        TransactionState.FAILED -> R.string.activity_details_label_failed
    }
)

private fun CustodialTransferActivitySummaryItem.statusStyle(): ActivityTagStyle = when (state) {
    TransactionState.COMPLETED -> ActivityTagStyle.Success
    TransactionState.PENDING -> ActivityTagStyle.Info
    TransactionState.FAILED -> ActivityTagStyle.Error
}

internal fun CustodialTransferActivitySummaryItem.buildActivityDetail() = CustodialActivityDetail(
    activity = this,
    extras = emptyList()
)