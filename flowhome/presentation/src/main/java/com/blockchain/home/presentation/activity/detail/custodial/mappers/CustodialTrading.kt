package com.blockchain.home.presentation.activity.detail.custodial.mappers

import com.blockchain.coincore.CustodialTradingActivitySummaryItem
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.domain.paymentmethods.model.MobilePaymentType
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtraKey
import com.blockchain.home.presentation.activity.detail.custodial.PaymentDetails
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.utils.abbreviate
import com.blockchain.utils.toFormattedString
import java.util.Currency
import java.util.Locale

internal fun CustodialTradingActivitySummaryItem.iconDetail(): ImageResource {
    return when (type) {
        OrderType.BUY,
        OrderType.RECURRING_BUY -> Icons.Filled.Plus

        OrderType.SELL -> Icons.Filled.Minus
    }
}

internal fun CustodialTradingActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = when (type) {
        OrderType.BUY,
        OrderType.RECURRING_BUY -> com.blockchain.stringResources.R.string.tx_title_bought

        OrderType.SELL -> com.blockchain.stringResources.R.string.tx_title_sold
    },
    args = listOf(account.currency.displayTicker)
)

internal fun CustodialTradingActivitySummaryItem.detailItems(
    extras: Map<CustodialActivityDetailExtraKey, CustodialActivityDetailExtra>
): List<ActivityDetailGroup> = listOf(
    // bought ----€10
    // to/from ---- euro
    ActivityDetailGroup(
        title = null,
        itemGroup = listOfNotNull(
            // Purchase ----€10
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(
                            when (type) {
                                OrderType.BUY,
                                OrderType.RECURRING_BUY ->
                                    com.blockchain.stringResources.R.string.activity_details_title_purchase

                                OrderType.SELL -> com.blockchain.stringResources.R.string.activity_details_title_sale
                            }
                        ),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
                        style = basicTitleStyle
                    )
                )
            ),
            // Amount ---- 0.00503823 BTC
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
            ),
            // btc price ---- $34,183.91
            price?.let { price ->
                ActivityComponent.StackView(
                    id = toString(),
                    leading = listOf(
                        ActivityStackView.Text(
                            value = TextValue.IntResValue(
                                value = com.blockchain.stringResources.R.string.quote_price,
                                args = listOf(
                                    when (type) {
                                        OrderType.BUY,
                                        OrderType.RECURRING_BUY -> account.currency.displayTicker

                                        OrderType.SELL -> fundedFiat.currencyCode
                                    }
                                )
                            ),
                            style = basicTitleStyle.muted()
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            value = TextValue.StringValue(price.toStringWithSymbol()),
                            style = basicTitleStyle
                        )
                    )
                )
            },
            // fee ---- €12
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_details_buy_fee),
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
    // to/from ---- euro
    ActivityDetailGroup(
        title = null,
        itemGroup = listOfNotNull(
            // status ---- success
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.common_status),
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
            // payment detail
            extras[CustodialActivityDetailExtraKey.PaymentDetail]?.toActivityComponent()
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

private fun CustodialTradingActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (state) {
        OrderState.FINISHED -> com.blockchain.stringResources.R.string.activity_details_label_complete
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.AWAITING_FUNDS,
        OrderState.PENDING_EXECUTION,
        OrderState.PENDING_CONFIRMATION -> when (type) {
            OrderType.BUY,
            OrderType.RECURRING_BUY -> {
                if (state == OrderState.AWAITING_FUNDS) {
                    com.blockchain.stringResources.R.string.activity_details_label_pending
                } else com.blockchain.stringResources.R.string.activity_details_label_pending_execution
            }

            OrderType.SELL -> com.blockchain.stringResources.R.string.activity_details_label_pending
        }

        OrderState.CANCELED -> com.blockchain.stringResources.R.string.activity_details_label_cancelled
        OrderState.UNKNOWN,
        OrderState.FAILED -> com.blockchain.stringResources.R.string.activity_details_label_failed
    }
)

private fun CustodialTradingActivitySummaryItem.statusStyle(): ActivityTagStyle = when (state) {
    OrderState.FINISHED -> ActivityTagStyle.Success
    OrderState.UNINITIALISED,
    OrderState.INITIALISED,
    OrderState.AWAITING_FUNDS,
    OrderState.PENDING_EXECUTION,
    OrderState.PENDING_CONFIRMATION -> ActivityTagStyle.Info

    OrderState.CANCELED -> ActivityTagStyle.Warning
    OrderState.UNKNOWN,
    OrderState.FAILED -> ActivityTagStyle.Error
}

internal fun PaymentDetails.toExtra() = CustodialActivityDetailExtra(
    title = TextValue.IntResValue(com.blockchain.stringResources.R.string.activity_details_buy_payment_method),
    value = with(this) {
        when {
            !endDigits.isNullOrEmpty() && !label.isNullOrEmpty() -> {
                accountType?.let {
                    TextValue.IntResValue(
                        value = com.blockchain.stringResources.R.string.common_spaced_strings,
                        args = listOf(
                            label,
                            TextValue.IntResValue(
                                value = com.blockchain.stringResources.R.string.payment_method_type_account_info,
                                args = listOf(accountType, endDigits)
                            )
                        )
                    )
                } ?: TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.common_hyphenated_strings,
                    args = listOf(label, endDigits)
                )
            }

            paymentMethodType == PaymentMethodType.PAYMENT_CARD &&
                endDigits.isNullOrEmpty() && label.isNullOrEmpty() -> {
                TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.credit_or_debit_card
                )
            }

            paymentMethodId == PaymentMethod.FUNDS_PAYMENT_ID -> {
                TextValue.StringValue(
                    value = label?.let {
                        Currency.getInstance(label).getDisplayName(Locale.getDefault())
                    } ?: ""
                )
            }

            mobilePaymentType == MobilePaymentType.GOOGLE_PAY -> {
                TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.google_pay
                )
            }

            mobilePaymentType == MobilePaymentType.APPLE_PAY -> {
                TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.apple_pay
                )
            }

            else -> {
                TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.activity_details_payment_load_fail
                )
            }
        }
    }
)

internal fun CustodialTradingActivitySummaryItem.buildActivityDetail(
    paymentDetails: PaymentDetails
) = CustodialActivityDetail(
    activity = this,
    extras = mapOf(
        CustodialActivityDetailExtraKey.PaymentDetail to paymentDetails.toExtra()
    )
)
