package com.blockchain.home.presentation.activity.detail.custodial.mappers

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blockchain.coincore.RecurringBuyActivitySummaryItem
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.recurringbuy.domain.RecurringBuyFrequency
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtra
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtraKey
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.muted
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.utils.abbreviate
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.utils.isLastDayOfTheMonth
import com.blockchain.utils.to12HourFormat
import com.blockchain.utils.toFormattedString
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@DrawableRes internal fun RecurringBuyActivitySummaryItem.iconDetail(): Int {
    return R.drawable.ic_activity_buy_dark
}

internal fun RecurringBuyActivitySummaryItem.title(): TextValue = TextValue.IntResValue(
    value = R.string.tx_title_bought,
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
                        value = TextValue.IntResValue(R.string.amount),
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
            ),
            // Total ----- Total
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.common_total),
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
    // from ---- Trading Account
    // to ---- 0x49...ba41
    ActivityDetailGroup(
        title = null,
        itemGroup = listOfNotNull(
            // status ---- success
            //            ActivityComponent.StackView(
            //                id = toString(),
            //                leading = listOfNotNull(
            //                    ActivityStackView.Text(
            //                        value = TextValue.IntResValue(R.string.common_status),
            //                        style = basicTitleStyle.muted()
            //                    ),
            //                    pendingConfirmations()?.let { pendingConfirmations: TextValue ->
            //                        ActivityStackView.Text(
            //                            value = pendingConfirmations,
            //                            style = basicSubtitleStyle.muted()
            //                        )
            //                    }
            //                ),
            //                trailing = listOf(
            //                    ActivityStackView.Tag(
            //                        value = statusValue(),
            //                        style = statusStyle()
            //                    )
            //                )
            //            ),
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.recurring_buy_frequency_label_1),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(
                            value = R.string.common_spaced_strings,
                            args = listOf(
                                "recurringBuyFrequency.title()", "frequency.value()"
                            )
                        ),
                        style = basicTitleStyle
                    )
                )
            ),
            ActivityComponent.StackView(
                id = toString(),
                leading = listOf(
                    ActivityStackView.Text(
                        value = TextValue.IntResValue(R.string.recurring_buy_details_next_payment),
                        style = basicTitleStyle.muted()
                    )
                ),
                trailing = listOf(
                    ActivityStackView.Text(
                        value = TextValue.StringValue("date.toFormattedString()"),
                        style = basicTitleStyle
                    )
                )
            ),
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

@StringRes fun RecurringBuyFrequency.title(): Int {
    return when (this) {
        RecurringBuyFrequency.ONE_TIME -> R.string.recurring_buy_one_time_selector
        RecurringBuyFrequency.DAILY -> R.string.recurring_buy_daily_1
        RecurringBuyFrequency.WEEKLY -> R.string.recurring_buy_weekly_1
        RecurringBuyFrequency.BI_WEEKLY -> R.string.recurring_buy_bi_weekly_1
        RecurringBuyFrequency.MONTHLY -> R.string.recurring_buy_monthly_1
        else -> R.string.common_unknown
    }
}

fun RecurringBuyFrequency.value(dateTime: ZonedDateTime): TextValue {
    return when (this) {
        RecurringBuyFrequency.DAILY -> {
            TextValue.IntResValue(
                value = R.string.recurring_buy_frequency_subtitle_each_day,
                args = listOf(dateTime.to12HourFormat())
            )
        }
        RecurringBuyFrequency.BI_WEEKLY, RecurringBuyFrequency.WEEKLY -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                TextValue.IntResValue(
                    value = R.string.recurring_buy_frequency_subtitle,
                    args = listOf(
                        dateTime.dayOfWeek
                            .getDisplayName(TextStyle.FULL, Locale.getDefault())
                            .toString().capitalizeFirstChar()
                    )
                )
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        }
        RecurringBuyFrequency.MONTHLY -> {
            if (dateTime.isLastDayOfTheMonth()) {
                TextValue.IntResValue(R.string.recurring_buy_frequency_subtitle_monthly_last_day)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TextValue.IntResValue(
                        value = R.string.recurring_buy_frequency_subtitle_monthly,
                        args = listOf(dateTime.dayOfMonth.toString())
                    )
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
            }
        }
        RecurringBuyFrequency.ONE_TIME,
        RecurringBuyFrequency.UNKNOWN -> TextValue.StringValue("")
    }
}

internal fun RecurringBuyActivitySummaryItem.buildActivityDetail() = CustodialActivityDetail(
    activity = this,
    extras = emptyMap()
)