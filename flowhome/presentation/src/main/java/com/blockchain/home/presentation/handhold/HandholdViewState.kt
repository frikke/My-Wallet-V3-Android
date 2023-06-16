package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.handhold.HandholdStepStatus

data class HandholdViewState(
    val stepsStatus: DataResource<List<HandholdStepStatus>>
) : ViewState {
    val showHandhold: DataResource<Boolean> get() = stepsStatus.map { it.any { !it.isComplete } }
}
