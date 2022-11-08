package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent

data class ActivityViewState(
    val activity: DataResource<Map<TransactionGroup, List<ActivityComponent>>>
) : ViewState

sealed interface TransactionGroup {
    object Combined : TransactionGroup

    sealed interface Group : TransactionGroup {
        data class Date(val date: String) : Group
        object Pending : Group
    }
}
