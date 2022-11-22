package com.blockchain.home.presentation.activity.list.custodial

import com.blockchain.coincore.ActivitySummaryItem
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityModelState
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.walletmode.WalletMode
import java.util.Calendar

class CustodialActivityViewModel(
    private val coincore: Coincore
) : MviViewModel<
    ActivityIntent<ActivitySummaryItem>,
    ActivityViewState,
    ActivityModelState<ActivitySummaryItem>,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ActivityModelState<ActivitySummaryItem>): ActivityViewState = state.run {
        ActivityViewState(
            activity = state.activityItems
                .filter { activityItem ->
                    if (state.filterTerm.isEmpty()) {
                        true
                    } else {
                        activityItem.matches(state.filterTerm)
                    }
                }
                .map { activityItems ->
                    activityItems.reduceActivityPage()
                }
                .map { groupedComponents ->
                    when (val sectionSize = state.sectionSize) {
                        SectionSize.All -> {
                            groupedComponents
                        }
                        is SectionSize.Limited -> {
                            mapOf(
                                TransactionGroup.Combined to groupedComponents.values.flatten().take(sectionSize.size)
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

    override suspend fun handleIntent(
        modelState: ActivityModelState<ActivitySummaryItem>,
        intent: ActivityIntent<ActivitySummaryItem>
    ) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }

                loadData()
            }

            is ActivityIntent.FilterSearch -> {
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
            .doOnSubscribe {
                updateState { it.copy(activityItems = DataResource.Loading) }
            }
            .subscribe { activity ->
                updateState { it.copy(activityItems = DataResource.Data(activity)) }
            }
    }
}

private fun ActivitySummaryItem.matches(filterTerm: String): Boolean {
    return account.currency.networkTicker.contains(filterTerm, ignoreCase = true) ||

        account.currency.name.contains(filterTerm, ignoreCase = true) ||

        value.toStringWithSymbol().contains(filterTerm, ignoreCase = true) ||

        (this as? CryptoActivitySummaryItem)?.asset?.let { asset ->
            asset.networkTicker.contains(filterTerm, ignoreCase = true) ||
                asset.name.contains(filterTerm, ignoreCase = true)
        } ?: false
}
