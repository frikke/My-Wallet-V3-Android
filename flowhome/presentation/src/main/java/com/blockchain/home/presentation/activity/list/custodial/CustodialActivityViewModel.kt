package com.blockchain.home.presentation.activity.list.custodial

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.custodial.list.toActivityComponent
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityIntent
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityModelState
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.walletmode.WalletMode
import java.util.Calendar

class CustodialActivityViewModel(
    private val coincore: Coincore
) : MviViewModel<
    CustodialActivityIntent,
    ActivityViewState,
    CustodialActivityModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(CustodialActivityModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: CustodialActivityModelState): ActivityViewState = state.run {
        ActivityViewState(
            activity = state.activity
                .map {
                    it.filter { activityItem ->
                        if (state.filterTerm.isEmpty()) {
                            true
                        } else {
                            activityItem.matches(state.filterTerm)
                        }
                    }
                }
                .map {
                    it.reduceActivityPage()
                }
                .map {
                    when (val sectionSize = state.sectionSize) {
                        SectionSize.All -> {
                            it
                        }
                        is SectionSize.Limited -> {
                            // todo do we need to filter out pending?
                            // todo make sure it's date sorted
                            mapOf(
                                TransactionGroup.Combined to it.values.flatten().take(sectionSize.size)
                            )
                        }
                    }
                }
        )
    }

    private fun List<ActivitySummaryItem>.reduceActivityPage(): Map<TransactionGroup, List<ActivityComponent>> {
        // group by date (month/year)
        return this
            .groupBy { activity ->
                val activityDate = Calendar.getInstance().apply { timeInMillis = activity.timeStampMs }
                if (activity.stateIsFinalised) {
                    Calendar.getInstance().apply {
                        timeInMillis = 0
                        set(Calendar.YEAR, activityDate.get(Calendar.YEAR))
                        set(Calendar.MONTH, activityDate.get(Calendar.MONTH))
                    }.let { date ->
                        TransactionGroup.Group.Date(date)
                    }
                } else {
                    TransactionGroup.Group.Pending
                }
            }
            // reduce to summary
            .map { (group, activities) ->
                group to activities.map { it.toActivityComponent() }
            }
            .toMap()
            .toSortedMap(compareByDescending { it })
    }

    override suspend fun handleIntent(modelState: CustodialActivityModelState, intent: CustodialActivityIntent) {
        when (intent) {
            is CustodialActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }

                loadData()
            }

            is CustodialActivityIntent.FilterSearch -> {
                updateState {
                    it.copy(filterTerm = intent.term)
                }
            }
        }
    }

    private fun loadData() {
        coincore.allWalletsInMode(WalletMode.CUSTODIAL_ONLY)
            .flatMap { accountGroup ->
                accountGroup.activity
            }
            .map {
                println("-------- size: ${it.size}")
                println("-------- value: ${it.map { it.value.toStringWithSymbol() }}")
                println("-------- filterIsInstance: ${it.filterIsInstance<FiatActivitySummaryItem>().size}")
                it
            }
            .doOnSubscribe {
                updateState { it.copy(activity = DataResource.Loading) }
            }
            .subscribe { activity ->
                updateState { it.copy(activity = DataResource.Data(activity)) }
            }
    }
}

private fun ActivitySummaryItem.matches(filterTerm: String): Boolean = true //todo