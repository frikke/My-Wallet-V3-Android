package com.blockchain.home.presentation.activity.list

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import java.util.Calendar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ActivityViewModel(
    private val unifiedActivityService: UnifiedActivityService
) : MviViewModel<
    ActivityIntent,
    ActivityViewState,
    ActivityModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: ActivityModelState): ActivityViewState = state.run {
        ActivityViewState(
            activity = state.activityPage
                .map {
                    it.copy(
                        activity = it.activity.filter { activityItem ->
                            if (state.filterTerm.isEmpty()) {
                                true
                            } else {
                                activityItem.summary.matches(state.filterTerm)
                            }
                        }
                    )
                }
                .map { it.reduceActivityPage() }
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

    private fun UnifiedActivityPage.reduceActivityPage(): Map<TransactionGroup, List<ActivityComponent>> {
        return activity
            // group by date (month/year)
            .groupBy { activity ->
                activity.date?.let {
                    it.apply {
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.let { date ->
                        TransactionGroup.Group.Date(date)
                    }
                } ?: TransactionGroup.Group.Pending
            }
            // reduce to summary
            .map { (group, activities) ->
                group to activities.map { it.summary.toActivityComponent() }
            }
            .toMap()
            .toSortedMap()
    }

    override suspend fun handleIntent(modelState: ActivityModelState, intent: ActivityIntent) {
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
            // todo(othman) use service with websocket
            unifiedActivityService
                .activityForAccount(
                    pubKey = "",
                    currency = "",
                    acceptLanguage = "",
                    timeZone = "",
                    nextPage = ""
                )
                .onEach { dataResource ->
                    updateState {
                        it.copy(activityPage = it.activityPage.updateDataWith(dataResource))
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
