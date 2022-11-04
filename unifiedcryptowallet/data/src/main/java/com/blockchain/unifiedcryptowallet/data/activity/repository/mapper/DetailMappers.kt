package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityDetailGroupsDto
import com.blockchain.api.selfcustody.activity.ActivityDetailGroupsDto.GroupedItems.DetailGroup
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroup
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDetailGroups

internal fun ActivityDetailGroupsDto.toActivityDetailGroups(): ActivityDetailGroups? = when (this) {
    is ActivityDetailGroupsDto.GroupedItems -> {
        ActivityDetailGroups.GroupedItems(
            title = title,
            subtitle = subtitle,
            icon = icon.toActivityIcon(),
            detailItems = items.map { it.toDetailGroup() },
            actionItems = floatingActions.mapNotNull { it.toActivityViewItem() }

        )
    }
    ActivityDetailGroupsDto.Unknown -> null
}

internal fun DetailGroup.toDetailGroup() = ActivityDetailGroup(
    title = title,
    itemGroup = itemGroup.mapNotNull { it.toActivityViewItem() }
)
