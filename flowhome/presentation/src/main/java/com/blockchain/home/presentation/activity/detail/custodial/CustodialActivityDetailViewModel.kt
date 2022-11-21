package com.blockchain.home.presentation.activity.detail.custodial

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.map
import com.blockchain.home.activity.CustodialActivityService
import com.blockchain.home.presentation.activity.common.toActivityComponent
import com.blockchain.home.presentation.activity.common.toStackedIcon
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailViewState
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CustodialActivityDetailViewModel(
    private val activityTxId: String,
    private val custodialActivityService: CustodialActivityService
) : MviViewModel<
    CustodialActivityDetailIntent,
    ActivityDetailViewState,
    CustodialActivityDetailModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(CustodialActivityDetailModelState()) {

    private var activityDetailJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: CustodialActivityDetailModelState): ActivityDetailViewState = state.run {
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
                detailItems = listOf(),
                floatingActions = actionItems.map { it.toActivityComponent() }
            )
        }
    }

    override suspend fun handleIntent(
        modelState: CustodialActivityDetailModelState,
        intent: CustodialActivityDetailIntent
    ) {
        when (intent) {
            is CustodialActivityDetailIntent.LoadActivityDetail -> {
                loadActivityDetail()
            }
        }
    }

    private fun loadActivityDetail() {
        activityDetailJob?.cancel()
        activityDetailJob = viewModelScope.launch {
            custodialActivityService
                .getActivity(txId = activityTxId)
                .onEach {
                    println("--------- ooo ${it.map { it is FiatActivitySummaryItem }}")
                }
                .collect()
            //            unifiedActivityService
            //                .getActivity(txId = activityTxId)
            //                .flatMapLatest { summaryDataResource ->
            //                    when (summaryDataResource) {
            //                        is DataResource.Data -> {
            //                            with(summaryDataResource.data) {
            //                                // todo(othman) real values
            //                                unifiedActivityService.getActivityDetails(
            //                                    txId = txId,
            //                                    network = network,
            //                                    pubKey = pubkey,
            //                                    locales = "en-GB;q=1.0, en",
            //                                    timeZone = "Europe/London"
            //                                )
            //                            }
            //                        }
            //                        is DataResource.Error -> flowOf(DataResource.Error(summaryDataResource.error))
            //                        DataResource.Loading -> flowOf(DataResource.Loading)
            //                    }
            //                }
            //                .onEach { dataResource ->
            //                    updateState {
            //                        it.copy(activityDetail = it.activityDetail.updateDataWith(dataResource))
            //                    }
            //                }
            //                .collect()
        }
    }
}
