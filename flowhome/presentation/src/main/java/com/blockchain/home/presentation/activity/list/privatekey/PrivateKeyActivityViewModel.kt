package com.blockchain.home.presentation.activity.list.privatekey

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.filter
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.activity.list.ActivityIntent
import com.blockchain.home.presentation.activity.list.ActivityModelState
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.activity.list.custodial.mappers.toActivityComponent
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityItem
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import java.util.Calendar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PrivateKeyActivityViewModel(
    private val unifiedActivityService: UnifiedActivityService
) : MviViewModel<
    ActivityIntent<UnifiedActivityItem>,
    ActivityViewState,
    ActivityModelState<UnifiedActivityItem>,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ActivityModelState<UnifiedActivityItem>): ActivityViewState = state.run {
        ActivityViewState(
            activity = state.activityItems
                .filter { activityItem ->
                    if (state.filterTerm.isEmpty()) {
                        true
                    } else {
                        activityItem.summary.matches(state.filterTerm)
                    }
                }
                .map { unifiedActivityItems ->
                    unifiedActivityItems.reduceActivityItems()
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

    private fun List<UnifiedActivityItem>.reduceActivityItems(): Map<TransactionGroup, List<ActivityComponent>> {
        // group by date (month/year)
        return groupBy { activity ->
            activity.date?.let {
                Calendar.getInstance().apply {
                    timeInMillis = 0
                    set(Calendar.YEAR, it.get(Calendar.YEAR))
                    set(Calendar.MONTH, it.get(Calendar.MONTH))
                }.let { date ->
                    TransactionGroup.Group.Date(date)
                }
            } ?: TransactionGroup.Group.Pending
        }
            // reduce to summary
            .map { (group, activities) ->
                group to activities.map { it.summary.toActivityComponent(componentId = it.txId) }
            }
            .toMap()
            .toSortedMap(compareByDescending { it })
    }

    override suspend fun handleIntent(
        modelState: ActivityModelState<UnifiedActivityItem>,
        intent: ActivityIntent<UnifiedActivityItem>
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
        viewModelScope.launch {
            unifiedActivityService
                // todo(othman) real values
                .getAllActivity(
                    acceptLanguage = "en-GB;q=1.0, en",
                    timeZone = "Europe/London"
                )
                .onEach { dataResource ->
                    updateState {
                        it.copy(activityItems = it.activityItems.updateDataWith(dataResource))
                    }
                }
                .collect()
        }
    }
}

private fun ActivityDataItem.matches(filterTerm: String): Boolean = when (this) {
    is ActivityDataItem.Stack -> {
        leading.any { stack -> stack.value.contains(filterTerm, ignoreCase = true) } ||
            trailing.any { stack -> stack.value.contains(filterTerm, ignoreCase = true) }
    }
    is ActivityDataItem.Button -> {
        value.contains(filterTerm, ignoreCase = true)
    }
}
