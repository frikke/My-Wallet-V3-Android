package com.blockchain.home.presentation.handhold

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.updateDataWith
import com.blockchain.home.handhold.HandholdService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HandholdViewModel(
    private val handholdService: HandholdService
) : MviViewModel<HandholdIntent, HandholdViewState, HandholdModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    HandholdModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun HandholdModelState.reduce() = HandholdViewState(
        stepsStatus = data
    )

    override suspend fun handleIntent(modelState: HandholdModelState, intent: HandholdIntent) {
        when (intent) {
            HandholdIntent.LoadData -> {
                loadData()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            handholdService.handholdTasksStatus().collectLatest {
                updateState {
                    copy(data = data.updateDataWith(it))
                }
            }
        }
    }
}