package com.blockchain.home.presentation.activity.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.activity.common.toStackedIcon
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups
import com.blockchain.unifiedcryptowallet.domain.activity.service.UnifiedActivityService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ActivityDetailViewModel(
    private val activityTxId: String,
    private val unifiedActivityService: UnifiedActivityService
) : MviViewModel<
    ActivityDetailIntent,
    ActivityDetailViewState,
    ActivityDetailModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityDetailModelState()) {

    private var activityDetailJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: ActivityDetailModelState): ActivityDetailViewState = state.run {
        ActivityDetailViewState(
            activityDetail = activityDetail.map { it.reduceActivityDetail() }
        )
    }

    private fun ActivityDetailGroups.reduceActivityDetail(): ActivityDetail = when (this) {
        is ActivityDetailGroups.GroupedItems -> {
            ActivityDetail(
                icon = icon.toStackedIcon(),
                title = title,
                subtitle = subtitle,
                detailItems = detailItems.map {
                    ActivityDetailGroup(
                        title = it.title,
                        itemGroup = it.itemGroup.map { it.toActivityComponent() }
                    )
                },
                floatingActions = actionItems.map { it.toActivityComponent() }
            )
        }
    }

    override suspend fun handleIntent(modelState: ActivityDetailModelState, intent: ActivityDetailIntent) {
        when (intent) {
            is ActivityDetailIntent.LoadActivityDetail -> {
                loadActivityDetail()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadActivityDetail() {
        activityDetailJob?.cancel()
        activityDetailJob = viewModelScope.launch {
            unifiedActivityService
                .getActivity(txId = activityTxId)
                .flatMapLatest { summaryDataResource ->
                    when (summaryDataResource) {
                        is DataResource.Data -> {
                            with(summaryDataResource.data) {
                                // todo(othman) real values
                                unifiedActivityService.getActivityDetails(
                                    txId = txId,
                                    network = network,
                                    pubKey = pubkey,
                                    locales = "en-GB;q=1.0, en",
                                    timeZone = "Europe/London"
                                )
                            }
                        }
                        is DataResource.Error -> flowOf(DataResource.Error(summaryDataResource.error))
                        DataResource.Loading -> flowOf(DataResource.Loading)
                    }
                }
                .onEach { dataResource ->
                    updateState {
                        it.copy(activityDetail = it.activityDetail.updateDataWith(dataResource))
                    }
                }
                .collect()
        }
    }
}
