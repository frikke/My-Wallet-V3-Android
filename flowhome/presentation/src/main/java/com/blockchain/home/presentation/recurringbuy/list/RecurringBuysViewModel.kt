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
import com.blockchain.utils.toFormattedDateWithoutYear
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
    ModelConfigArgs.NoArgs>(initialState = RecurringBuysModelState()) {
    private var recurringBuysJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: RecurringBuysModelState): RecurringBuysViewState = state.run {
        RecurringBuysViewState(
            recurringBuys = state.recurringBuys
                .map { it?.take(sectionSize.size) }
                .map {
                    it?.let { recurringBuys ->
                        RecurringBuyEligibleState.Eligible(
                            recurringBuys = recurringBuys
                                .sortedBy { it.nextPaymentDate }
                                .map { recurringBuy ->
                                    RecurringBuyViewState(
                                        id = recurringBuy.id,
                                        iconUrl = recurringBuy.asset.logo,
                                        description = TextValue.IntResValue(
                                            R.string.dashboard_recurring_buy_item_title_1,
                                            listOf(
                                                recurringBuy.amount.toStringWithSymbol(),
                                                recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy()
                                            )
                                        ),
                                        status = if (recurringBuy.state == RecurringBuyState.ACTIVE) {
                                            TextValue.IntResValue(
                                                R.string.dashboard_recurring_buy_item_label,
                                                listOf(recurringBuy.nextPaymentDate.toFormattedDateWithoutYear())
                                            )
                                        } else {
                                            TextValue.IntResValue(
                                                R.string.dashboard_recurring_buy_item_label_error
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

    @StringRes private fun RecurringBuyFrequency.toHumanReadableRecurringBuy(): Int {
        return when (this) {
            RecurringBuyFrequency.ONE_TIME -> R.string.recurring_buy_one_time_selector
            RecurringBuyFrequency.DAILY -> R.string.recurring_buy_daily_1
            RecurringBuyFrequency.WEEKLY -> R.string.recurring_buy_weekly_1
            RecurringBuyFrequency.BI_WEEKLY -> R.string.recurring_buy_bi_weekly_1
            RecurringBuyFrequency.MONTHLY -> R.string.recurring_buy_monthly_1
            else -> R.string.common_unknown
        }
    }
}
