package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.components.ActivityStackView

data class ActivityViewState(
    val activity: DataResource<Map<TransactionGroup, List<ActivityStackView>>>
) : ViewState

sealed interface TransactionGroup {
    val name: String

    object Combined : TransactionGroup {
        override val name get() = error("not allowed")
    }

    data class Group(override val name: String) : TransactionGroup
}
