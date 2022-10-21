package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.dashboard.HomeNavEvent

class ActivityDetailViewModel(
    // todo
) : MviViewModel<
    ActivityDetailIntent,
    ActivityDetailViewState,
    ActivityDetailModelState,
    HomeNavEvent,
    ModelConfigArgs.NoArgs>(ActivityDetailModelState()) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: ActivityDetailModelState): ActivityDetailViewState = state.run {
        ActivityDetailViewState(
            activity = activity
        )
    }

    override suspend fun handleIntent(modelState: ActivityDetailModelState, intent: ActivityDetailIntent) {
        when (intent) {
            is ActivityDetailIntent.LoadActivityDetail -> {
                updateState {
                    it.copy(
                        activity = DataResource.Data(
                            listOf(
                                listOf(
                                    ActivityDetailItemState(
                                        ViewType.KeyValue,
                                        "Purchase",
                                        "100.00",
                                        ValueStyle.Text
                                    ),
                                    ActivityDetailItemState(
                                        ViewType.KeyValue,
                                        "BTC Price",
                                        "34,183.91",
                                        ValueStyle.Text
                                    ),
                                    ActivityDetailItemState(
                                        ViewType.KeyValue,
                                        "Fees",
                                        "Free",
                                        ValueStyle.GreenText
                                    )
                                ),
                                listOf(
                                    ActivityDetailItemState(
                                        ViewType.KeyValue,
                                        "Status",
                                        "Complete",
                                        ValueStyle.SuccessBadge
                                    ),
                                    ActivityDetailItemState(
                                        ViewType.KeyValue,
                                        "Type",
                                        "Easy Bank Transfer",
                                        ValueStyle.Text
                                    )
                                ),
                            )
                        )
                    )
                }
            }
        }
    }
}
