package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
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

@DrawableRes internal fun FiatActivitySummaryItem.iconDetail(): Int {
    return when (type) {
        TransactionType.DEPOSIT -> R.drawable.ic_activity_buy_dark
        TransactionType.WITHDRAWAL -> R.drawable.ic_activity_sell_dark
    }
}

internal fun FiatActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = when (type) {
        TransactionType.DEPOSIT -> R.string.tx_title_deposited
        TransactionType.WITHDRAWAL -> R.string.tx_title_withdrawn
    },
    args = listOf(account.currency.displayTicker)
)

internal fun FiatActivitySummaryItem.detailItems(
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
                        value = TextValue.IntResValue(
                            when (type) {
                                TransactionType.DEPOSIT -> R.string.common_deposit
                                TransactionType.WITHDRAWAL -> R.string.fiat_funds_detail_withdraw_title
                            }
                        ),
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

            // to/from ---- euro
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(
                            when (type) {
                                TransactionType.DEPOSIT -> R.string.common_to
                                TransactionType.WITHDRAWAL -> R.string.common_from
                            }
                        ),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(account.label),
                        style = basicTitleStyle
                    )
                )
            )
        )
    ),
    // status ---- success
    // to/from ---- euro
    ActivityDetailGroup(
        title = null,
        itemGroup = listOf(
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

private fun FiatActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (state) {
        TransactionState.COMPLETED -> R.string.activity_details_completed
        TransactionState.PENDING -> R.string.activity_details_label_pending
        TransactionState.FAILED -> R.string.activity_details_label_failed
    }
)

private fun FiatActivitySummaryItem.statusStyle(): ActivityTagStyle = when (state) {
    TransactionState.COMPLETED -> ActivityTagStyle.Success
    TransactionState.PENDING -> ActivityTagStyle.Info
    TransactionState.FAILED -> ActivityTagStyle.Error
}

internal fun FiatActivitySummaryItem.buildActivityDetail(
    paymentMethod: PaymentMethodDetails
) = CustodialActivityDetail(
    activity = this,
    extras = listOf(
        CustodialActivityDetailExtra(
            title = TextValue.IntResValue(R.string.activity_details_buy_payment_method),
            value = with(paymentMethod) {
                when {
                    mobilePaymentType == MobilePaymentType.GOOGLE_PAY -> TextValue.IntResValue(
                        R.string.google_pay
                    )
                    mobilePaymentType == MobilePaymentType.APPLE_PAY -> TextValue.IntResValue(
                        R.string.apple_pay
                    )
                    label.isNullOrBlank() -> TextValue.StringValue(
                        account.currency.name
                    )
                    else -> TextValue.StringValue(
                        "$label $endDigits"
                    )
                }
            }
        )
    )
)
