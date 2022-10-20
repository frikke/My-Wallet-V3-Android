package com.blockchain.home.presentation.activity

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.home.presentation.dashboard.HomeNavEvent

class ActivityViewModel(
) : MviViewModel<ActivityIntent, ActivityViewState, ActivityModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
    ActivityModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: ActivityModelState): ActivityViewState = state.run {
        ActivityViewState(
            activity = activity
        )
    }

    override suspend fun handleIntent(modelState: ActivityModelState, intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
            }
        }
    }
}
