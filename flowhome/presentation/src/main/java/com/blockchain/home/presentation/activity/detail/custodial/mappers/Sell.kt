package com.blockchain.home.presentation.activity.detail.custodial.mappers

import androidx.annotation.DrawableRes
import com.blockchain.coincore.NullCryptoAddress.asset
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityButtonStyleState
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTagStyleState
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted
import com.blockchain.nabu.datamanagers.CustodialOrderState
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.utils.abbreviate
import com.blockchain.utils.toFormattedString
import info.blockchain.balance.Money

@DrawableRes internal fun TradeActivitySummaryItem.sellIconDetail(): Int {
    return R.drawable.ic_activity_sell
}

internal fun TradeActivitySummaryItem.sellTitle(): TextValue = TextValue.IntResValue(
    value = R.string.tx_title_sold,
    args = listOf(
        currencyPair.source.displayTicker
    )
)

internal fun TradeActivitySummaryItem.sellDetailItems(
    extras: List<CustodialActivityDetailExtra>
): List<ActivityDetailGroup> = listOf(
    // deposit ----€10
    // to/from ---- euro
    ActivityDetailGroup(
        title = null,
        itemGroup = listOfNotNull(
            // deposit ----€10
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.activity_details_title_sale),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(receivingValue.toStringWithSymbol()),
                        style = basicTitleStyle
                    )
                )
            ),
            // Amount ----0.00503823 BTC
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
                        value = TextValue.StringValue(sendingValue.toStringWithSymbol()),
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
                                value = R.string.quote_price,
                                args = listOf(account.currency.displayTicker)
                            ),
                            style = basicTitleStyle.muted()
                        )
                    ),
                    trailing = listOf(
                        ActivityStackView.Text(
                            value = TextValue.IntResValue(
                                value = R.string.activity_details_exchange_rate_value,
                                args = listOf(price.toStringWithSymbol(), sendingValue.currencyCode)
                            ),
                            style = basicTitleStyle
                        )
                    )
                )
            },
            // fee ---- €12
            *extras.map { it.toActivityComponent() }.toTypedArray()
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
                        value = TextValue.StringValue(
                            "${currencyPair.source.displayTicker} ${sendingAccount.label}"
                        ),
                        style = basicTitleStyle
                    )
                )
            ),
            // to ---- 0x49...ba41
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
                        value = TextValue.StringValue(
                            "${currencyPair.destination} ${sendingAccount.label}"
                        ),
                        style = basicTitleStyle
                    )
                )
            )
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
                style = ActivityButtonStyleState.Tertiary,
                action = ActivityButtonAction(
                    type = ActivityButtonAction.ActivityButtonActionType.Copy,
                    data = txId
                )
            )
        )
    )
)

private fun TradeActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (state) {
        CustodialOrderState.FINISHED -> R.string.activity_details_completed
        CustodialOrderState.CREATED,
        CustodialOrderState.PENDING_EXECUTION,
        CustodialOrderState.PENDING_CONFIRMATION,
        CustodialOrderState.PENDING_LEDGER,
        CustodialOrderState.PENDING_DEPOSIT,
        CustodialOrderState.PENDING_WITHDRAWAL,
        CustodialOrderState.FINISH_DEPOSIT -> R.string.activity_details_label_pending
        CustodialOrderState.CANCELED -> R.string.activity_details_label_cancelled
        CustodialOrderState.EXPIRED,
        CustodialOrderState.UNKNOWN,
        CustodialOrderState.FAILED -> R.string.activity_details_label_failed
    }
)

private fun TradeActivitySummaryItem.statusStyle(): ActivityTagStyleState = when (state) {
    CustodialOrderState.FINISHED -> ActivityTagStyleState.Success
    CustodialOrderState.CREATED,
    CustodialOrderState.PENDING_EXECUTION,
    CustodialOrderState.PENDING_CONFIRMATION,
    CustodialOrderState.PENDING_LEDGER,
    CustodialOrderState.PENDING_DEPOSIT,
    CustodialOrderState.PENDING_WITHDRAWAL,
    CustodialOrderState.FINISH_DEPOSIT -> ActivityTagStyleState.Info
    CustodialOrderState.CANCELED -> ActivityTagStyleState.Warning
    CustodialOrderState.EXPIRED,
    CustodialOrderState.UNKNOWN,
    CustodialOrderState.FAILED -> ActivityTagStyleState.Error
}

internal fun TradeActivitySummaryItem.buildASellActivityDetail(
    fee: Money
) = CustodialActivityDetail(
    activity = this,
    extras = listOf(
        CustodialActivityDetailExtra(
            title = TextValue.IntResValue(R.string.activity_details_buy_fee),
            value = TextValue.StringValue(fee.toStringWithSymbol())
        )
    )
)
