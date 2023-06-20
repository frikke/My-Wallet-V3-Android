package com.blockchain.home.presentation.handhold

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.handhold.HandholdTasksStatus

data class HandholdViewState(
    val tasksStatus: DataResource<List<HandholdTasksStatus>>,
    val showHandhold: DataResource<Boolean>
) : ViewState
