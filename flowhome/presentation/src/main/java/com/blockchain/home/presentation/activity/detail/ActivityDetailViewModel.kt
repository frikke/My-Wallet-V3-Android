package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.home.presentation.activity.detail.composable.DETAIL_DUMMY_DATA
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
                        activity = DETAIL_DUMMY_DATA
                    )
                }
            }
        }
    }
}
