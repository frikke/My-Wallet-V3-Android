package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.map
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.activity.list.composable.DUMMY_DATA
import com.blockchain.home.presentation.dashboard.HomeNavEvent

class ActivityViewModel(
    // todo
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
            activity = activity.map {
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

    override suspend fun handleIntent(modelState: ActivityModelState, intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadActivity -> {
                updateState { it.copy(sectionSize = intent.sectionSize) }

                updateState {
                    it.copy(
                        activity = DUMMY_DATA
                    )
                }
            }
        }
    }
}
