package com.blockchain.home.presentation.activity.detail.custodial.mappers

import com.blockchain.home.presentation.activity.common.ActivityComponent
import com.blockchain.home.presentation.activity.common.ActivityIconState
import com.blockchain.home.presentation.activity.common.ActivityStackView
import com.blockchain.home.presentation.activity.detail.ActivityDetail
import com.blockchain.home.presentation.activity.detail.ActivityDetailGroup
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetail
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailExtras
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicSubtitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.basicTitleStyle
import com.blockchain.home.presentation.activity.list.custodial.mappers.icon

fun CustodialActivityDetail.toActivityDetail(): ActivityDetail {
    return ActivityDetail(
        icon = ActivityIconState.SingleIcon.Local(activity.icon()),
        title = "",
        subtitle = "",
        detailItems = listOf(
            ActivityDetailGroup(
                title = null,
                itemGroup = listOf<ActivityComponent>() + extras.map { it.toActivityComponent() }
            )
        ),
        floatingActions = listOf()
    )
}

/**
 * map extras to ActivityStackView
 */
private fun CustodialActivityDetailExtras.toActivityComponent() = ActivityComponent.StackView(
    id = this.toString(),
    leading = listOf(
        ActivityStackView.Text(
            value = title,
            style = basicTitleStyle
        )
    ),
    trailing = listOf(
        ActivityStackView.Text(
            value = value,
            style = basicSubtitleStyle
        )
    )
)