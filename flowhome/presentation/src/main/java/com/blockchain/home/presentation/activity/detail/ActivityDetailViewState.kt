package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class ActivityDetailViewState(
    val activity: DataResource<List<List<ActivityDetailItemState>>>
) : ViewState

sealed interface ActivityDetailItemState{
    data class KeyValue(
        val key: String,
        val value: String,
        val valueStyle: ValueStyle
    ): ActivityDetailItemState

    data class Button(
        val value: String
    ): ActivityDetailItemState
}

enum class ValueStyle {
    SuccessBadge,
    GreenText,
    Text
}