package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class ActivityDetailViewState(
    val activity: DataResource<List<List<ActivityDetailItemState>>>
) : ViewState

data class ActivityDetailItemState(
    val type: ViewType,
    val key: String,
    val value: String,
    val valueStyle: ValueStyle
)

enum class ViewType {
    KeyValue, Button
}

enum class ValueStyle {
    SuccessBadge,
    GreenText,
    Text
}