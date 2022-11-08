package com.blockchain.home.presentation.activity.list

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.common.ActivityButtonStyleState
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.common.ActivityTagStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextColorState
import com.blockchain.home.presentation.activity.common.ActivityTextStyleState
import com.blockchain.home.presentation.activity.common.ActivityTextTypographyState
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography
import com.blockchain.unifiedcryptowallet.domain.activity.model.StackComponent
import com.blockchain.unifiedcryptowallet.domain.activity.model.UnifiedActivityPage
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar

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
            activity = state.activityPage.map { it.reduceActivityPage() }.map {
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
                    Calendar.getInstance().apply {
                        set(Calendar.MONTH, it.get(Calendar.MONTH))
                        set(Calendar.YEAR, it.get(Calendar.YEAR))
                    }.let {
                        TransactionGroup.Group.Date("${it.get(Calendar.MONTH)} ${it.get(Calendar.YEAR)}")
                    }
                } ?: TransactionGroup.Group.Pending
            }
            // reduce to summary
            .map { (group, activities) ->
                group to activities.map { it.summary.toActivityComponent() }
            }
            .toMap()
    }

    override suspend fun handleIntent(modelState: ActivityModelState, intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }

                loadData()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
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