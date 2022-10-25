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
            activityDetailItems = activity
        )
    }

    override suspend fun handleIntent(modelState: ActivityDetailModelState, intent: ActivityDetailIntent) {
        when (intent) {
            is ActivityDetailIntent.LoadActivityDetail -> {
                updateState {
                    it.copy(
                        activity = DataResource.Data(
                            ActivityDetail(
                                itemGroups = listOf(
                                    listOf(
                                        ActivityDetailItemState.KeyValue(
                                            "Purchase",
                                            "100.00",
                                            ValueStyle.Text
                                        ),
                                        ActivityDetailItemState.KeyValue(
                                            "BTC Price",
                                            "34,183.91",
                                            ValueStyle.Text
                                        ),
                                        ActivityDetailItemState.KeyValue(
                                            "Fees",
                                            "Free",
                                            ValueStyle.GreenText
                                        ),
                                        ActivityDetailItemState.Button(
                                            "Copy Transaction ID",
                                            ButtonStyle.Primary
                                        )
                                    ),
                                    listOf(
                                        ActivityDetailItemState.KeyValue(
                                            "Status",
                                            "Complete",
                                            ValueStyle.SuccessBadge
                                        ),
                                        ActivityDetailItemState.KeyValue(
                                            "Type",
                                            "Easy Bank Transfer",
                                            ValueStyle.Text
                                        ),
                                        ActivityDetailItemState.Button(
                                            "Copy Transaction ID",
                                            ButtonStyle.Tertiary
                                        )
                                    ),
                                ),
                                floatingActions = listOf(
                                    ActivityDetailItemState.Button(
                                        "View on Etherscan",
                                        ButtonStyle.Primary
                                    ),
                                    ActivityDetailItemState.Button(
                                        "Speed Up",
                                        ButtonStyle.Secondary
                                    ),
                                    ActivityDetailItemState.Button(
                                        "Cancel",
                                        ButtonStyle.Tertiary
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
