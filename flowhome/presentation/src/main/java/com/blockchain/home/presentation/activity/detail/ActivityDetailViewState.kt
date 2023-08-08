package com.blockchain.home.presentation.activity.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.image.LogoValue

data class ActivityDetailViewState(
    val activityDetail: DataResource<ActivityDetail>
) : ViewState

data class ActivityDetail(
    val icon: LogoValue,
    val title: TextValue,
    val subtitle: TextValue,
    val detailItems: List<ActivityDetailGroup>,
    val floatingActions: List<ActivityComponent>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityDetail

        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (detailItems != other.detailItems) return false
        if (floatingActions != other.floatingActions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + subtitle.hashCode()
        result = 31 * result + detailItems.hashCode()
        result = 31 * result + floatingActions.hashCode()
        return result
    }
}

data class ActivityDetailGroup(
    val title: String?,
    val itemGroup: List<ActivityComponent>
)
