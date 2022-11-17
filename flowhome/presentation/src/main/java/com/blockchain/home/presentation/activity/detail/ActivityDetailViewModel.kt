package com.blockchain.home.presentation.activity.detail

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.dashboard.HomeNavEvent
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

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        println("----------activityTxId $activityTxId")
    }

    override fun reduce(state: ActivityDetailModelState): ActivityDetailViewState = state.run {
        ActivityDetailViewState(
            activityDetailItems = activity
        )
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
        println("---------- getActivity $activityTxId")

        activityDetailJob?.cancel()
        activityDetailJob = viewModelScope.launch {
            unifiedActivityService
                .getActivity(txId = activityTxId)
                .onEach {
                    println("---------- getActivity $it")
                }
                .flatMapLatest { summaryDataResource ->
                    when (summaryDataResource) {
                        is DataResource.Data -> {
                            with(summaryDataResource.data){
                                unifiedActivityService.getActivityDetails(
                                    txId= txId,
                                    network = network,
                                    pubKey = txId,
                                    locales  = "en-GB;q=1.0, en",
                                    timeZone = "Europe/London"
                                )
                            }
                        }
                        is DataResource.Error -> flowOf(DataResource.Error(summaryDataResource.error))
                        DataResource.Loading -> flowOf(DataResource.Loading)
                    }
                }
                .onEach {
                    println("---------- $it")
                }
                .collect()
        }
    }
}
