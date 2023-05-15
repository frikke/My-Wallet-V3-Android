package com.blockchain.home.presentation.recurringbuy.list

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyFrequency
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.utils.capitalizeFirstChar
import com.blockchain.utils.isLastDayOfTheMonth
import com.blockchain.utils.to12HourFormat
import com.blockchain.utils.toFormattedDateWithoutYear
import com.google.protobuf.StringValue
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecurringBuysViewModel(
    private val recurringBuyService: RecurringBuyService
) : MviViewModel<
    RecurringBuysIntent,
    RecurringBuysViewState,
    RecurringBuysModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs
    >(initialState = RecurringBuysModelState()) {
    private var recurringBuysJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: RecurringBuysModelState): RecurringBuysViewState = state.run {
        RecurringBuysViewState(
            recurringBuys = state.recurringBuys
                .map {
                    it?.let { recurringBuys ->
                        RecurringBuyEligibleState.Eligible(
                            recurringBuys = recurringBuys
                                .sortedBy { it.nextPaymentDate }
                                .take(sectionSize.size)
                                .map { recurringBuy ->
                                    RecurringBuyViewState(
                                        id = recurringBuy.id,
                                        assetTicker = recurringBuy.asset.networkTicker,
                                        iconUrl = recurringBuy.asset.logo,
                                        description = TextValue.IntResValue(
                                            com.blockchain.stringResources.R.string
                                                .dashboard_recurring_buy_item_title_1,
                                            listOf(
                                                recurringBuy.amount.toStringWithSymbol(),
                                                recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                                            )
                                        ),
                                        status = if (recurringBuy.state == RecurringBuyState.ACTIVE) {
                                            TextValue.IntResValue(
                                                com.blockchain.stringResources.R
                                                    .string.dashboard_recurring_buy_item_label,
                                                listOf(recurringBuy.nextPaymentDate.toFormattedDateWithoutYear())
                                            )
                                        } else {
                                            TextValue.IntResValue(
                                                com.blockchain.stringResources.R
                                                    .string.dashboard_recurring_buy_item_label_error
                                            )
                                        }
                                    )
                                }
                        )
                    } ?: RecurringBuyEligibleState.NotEligible
                }
        )
    }

    override suspend fun handleIntent(
        modelState: RecurringBuysModelState,
        intent: RecurringBuysIntent
    ) {
        when (intent) {
            is RecurringBuysIntent.LoadRecurringBuys -> {
                updateState {
                    it.copy(
                        sectionSize = intent.sectionSize
                    )
                }

                loadRecurringBuys(includeInactive = intent.includeInactive)
            }
        }
    }

    private fun loadRecurringBuys(
        includeInactive: Boolean = false
    ) {
        recurringBuysJob?.cancel()
        recurringBuysJob = viewModelScope.launch {
            if (recurringBuyService.isEligible()) {
                recurringBuyService.recurringBuys(includeInactive = includeInactive)
                    .collectLatest { recurringBuys ->
                        updateState {
                            it.copy(recurringBuys = it.recurringBuys.updateDataWith(recurringBuys))
                        }
                    }
            } else {
                updateState {
                    it.copy(recurringBuys = DataResource.Data(null))
                }
            }
        }
    }
}

@StringRes fun RecurringBuyFrequency.toHumanReadableRecurringBuy(): Int {
    return when (this) {
        RecurringBuyFrequency.ONE_TIME -> com.blockchain.stringResources.R.string.recurring_buy_one_time_selector
        RecurringBuyFrequency.DAILY -> com.blockchain.stringResources.R.string.recurring_buy_daily_1
        RecurringBuyFrequency.WEEKLY -> com.blockchain.stringResources.R.string.recurring_buy_weekly_1
        RecurringBuyFrequency.BI_WEEKLY -> com.blockchain.stringResources.R.string.recurring_buy_bi_weekly_1
        RecurringBuyFrequency.MONTHLY -> com.blockchain.stringResources.R.string.recurring_buy_monthly_1
        else -> com.blockchain.stringResources.R.string.common_unknown
    }
}

fun RecurringBuyFrequency.toHumanReadableRecurringDate(dateTime: ZonedDateTime): TextValue {
    return when (this) {
        RecurringBuyFrequency.DAILY -> {
            TextValue.IntResValue(
                com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle_each_day,
                listOf(dateTime.to12HourFormat())
            )
        }

        RecurringBuyFrequency.BI_WEEKLY, RecurringBuyFrequency.WEEKLY -> {
            TextValue.IntResValue(
                com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle,
                listOf(
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
                    com.blockchain.stringResources.R.string.recurring_buy_frequency_subtitle_monthly,
                    listOf(dateTime.dayOfMonth.toString())
                )
            }
        }

        RecurringBuyFrequency.ONE_TIME,
        RecurringBuyFrequency.UNKNOWN -> TextValue.StringValue("")
    }
}
