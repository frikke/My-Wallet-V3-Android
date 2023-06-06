package com.blockchain.home.presentation.activity.detail.custodial.mappers

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
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
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityLocalIcon
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.utils.abbreviate
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.utils.isLastDayOfTheMonth
import com.blockchain.utils.to12HourFormat
import com.blockchain.utils.toFormattedString
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

internal fun RecurringBuyActivitySummaryItem.iconDetail(): ActivityLocalIcon {
    return ActivityLocalIcon.Buy
}

internal fun RecurringBuyActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = com.blockchain.stringResources.R.string.tx_title_bought,
    args = listOf(account.currency.displayTicker)
)

internal fun RecurringBuyActivitySummaryItem.detailItems(
    extras: Map<CustodialActivityDetailExtraKey, CustodialActivityDetailExtra>
): List<ActivityDetailGroup> = listOf(
    // amount ----€10
    ActivityDetailGroup(
        title = null,
        itemGroup = listOf(
            // amount ----€10
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
                        value = TextValue.StringValue(
                            if (value.isPositive) {
                                value
                            } else {
                                fundedFiat
                            }.toStringWithSymbol()
                        ),
                        style = basicTitleStyle
                    )
                )
            ),
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
            ),
            // Total ----- Total
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(com.blockchain.stringResources.R.string.common_total),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue(fundedFiat.toStringWithSymbol()),
                        style = basicTitleStyle
                    )
                )
            )
        )
    ),
    // status ---- success
    // frequency ---- Twice a Month On Thursday
    // next payment date ---- 01:41 PM on Nov 3, 2022
    // Payment Method ---- MASTERCARD EUROPE - 0029
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
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Tag(
                        value = statusValue(),
                        style = statusStyle()
                    )
                )
            ),

            // frequency ---- Twice a Month On Thursday
            extras[CustodialActivityDetailExtraKey.Frequency]?.toActivityComponent(),

            // next payment date ---- 01:41 PM on Nov 3, 2022
            extras[CustodialActivityDetailExtraKey.NextPaymentDate]?.toActivityComponent(),

            // Payment Method ---- MASTERCARD EUROPE - 0029
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

@StringRes fun RecurringBuyFrequency.title(): Int {
    return when (this) {
        RecurringBuyFrequency.ONE_TIME -> com.blockchain.stringResources.R.string.recurring_buy_one_time_selector
        RecurringBuyFrequency.DAILY -> com.blockchain.stringResources.R.string.recurring_buy_daily_1
        RecurringBuyFrequency.WEEKLY -> com.blockchain.stringResources.R.string.recurring_buy_weekly_1
        RecurringBuyFrequency.BI_WEEKLY -> com.blockchain.stringResources.R.string.recurring_buy_bi_weekly_1
        RecurringBuyFrequency.MONTHLY -> com.blockchain.stringResources.R.string.recurring_buy_monthly_1
        else -> com.blockchain.stringResources.R.string.common_unknown
    }
}

@SuppressLint("NewApi") // todo fix for < api 26
fun RecurringBuyFrequency.value(date: Date): TextValue {
    val dateTime = ZonedDateTime.ofInstant(
        date.toInstant(),
        ZoneId.systemDefault()
    )
    return when (this) {
        RecurringBuyFrequency.DAILY -> {
            TextValue.IntResValue(
                value = com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle_each_day,
                args = listOf(dateTime.to12HourFormat())
            )
        }
        RecurringBuyFrequency.BI_WEEKLY, RecurringBuyFrequency.WEEKLY -> {
            TextValue.IntResValue(
                value = com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle,
                args = listOf(
                    dateTime.dayOfWeek
                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .toString().capitalizeFirstChar()
                )
            )
        }
        RecurringBuyFrequency.MONTHLY -> {
            if (dateTime.isLastDayOfTheMonth()) {
                TextValue.IntResValue(
                    com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle_monthly_last_day
                )
            } else {
                TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle_monthly,
                    args = listOf(dateTime.dayOfMonth.toString())
                )
            }
        }
        RecurringBuyFrequency.ONE_TIME,
        RecurringBuyFrequency.UNKNOWN -> TextValue.StringValue("")
    }
}

private fun RecurringBuyActivitySummaryItem.statusValue(): TextValue = TextValue.IntResValue(
    when (transactionState) {
        OrderState.FINISHED -> com.blockchain.stringResources.R.string.activity_details_label_complete
        OrderState.PENDING_CONFIRMATION,
        OrderState.PENDING_EXECUTION,
        OrderState.AWAITING_FUNDS -> com.blockchain.stringResources.R.string.activity_details_label_confirming
        OrderState.CANCELED -> com.blockchain.stringResources.R.string.activity_details_label_cancelled
        OrderState.UNINITIALISED,
        OrderState.INITIALISED,
        OrderState.UNKNOWN,
        OrderState.FAILED -> com.blockchain.stringResources.R.string.activity_details_label_failed
    }
)

private fun RecurringBuyActivitySummaryItem.statusStyle(): ActivityTagStyle = when (transactionState) {
    OrderState.FINISHED -> ActivityTagStyle.Success
    OrderState.PENDING_CONFIRMATION,
    OrderState.PENDING_EXECUTION,
    OrderState.AWAITING_FUNDS -> ActivityTagStyle.Info
    OrderState.CANCELED -> ActivityTagStyle.Warning
    OrderState.UNINITIALISED,
    OrderState.INITIALISED,
    OrderState.UNKNOWN,
    OrderState.FAILED -> ActivityTagStyle.Error
}

internal fun RecurringBuyActivitySummaryItem.buildActivityDetail(
    recurringBuy: RecurringBuy?,
    paymentDetails: PaymentDetails
): CustodialActivityDetail {
    val rbExtras = recurringBuy?.let {
        mapOf(
            CustodialActivityDetailExtraKey.NextPaymentDate to CustodialActivityDetailExtra(
                title = TextValue.IntResValue(
                    com.blockchain.stringResources.R.string.recurring_buy_details_next_payment
                ),
                value = TextValue.StringValue(recurringBuy.nextPaymentDate.toFormattedString())
            ),
            CustodialActivityDetailExtraKey.Frequency to CustodialActivityDetailExtra(
                title = TextValue.IntResValue(com.blockchain.stringResources.R.string.recurring_buy_frequency_label_1),
                value = TextValue.IntResValue(
                    value = com.blockchain.stringResources.R.string.common_spaced_strings,
                    args = listOf(
                        recurringBuy.recurringBuyFrequency.title(),
                        recurringBuy.recurringBuyFrequency.value(recurringBuy.nextPaymentDate)
                    )
                )
            )
        )
    } ?: emptyMap()

    val paymentExtras = mapOf(CustodialActivityDetailExtraKey.PaymentDetail to paymentDetails.toExtra())

    return CustodialActivityDetail(
        activity = this,
        extras = rbExtras + paymentExtras
    )
}
