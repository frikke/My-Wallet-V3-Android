package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityViewItemDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityDataItem

internal fun ActivityViewItemDto.toActivityViewItem(): ActivityDataItem? = when (this) {
    is ActivityViewItemDto.Stack -> {
        ActivityDataItem.Stack(
            leadingImage = leadingImage.toActivityIcon(),
            leadingImageDark = (leadingImageDark ?: leadingImage).toActivityIcon(),
            leading = leading.mapNotNull { it.toStackComponent() },
            trailing = trailing.mapNotNull { it.toStackComponent() }
        )
    }
    is ActivityViewItemDto.Button -> {
        // ignore if the action is not defined
        toButtonAction()?.let { action ->
            ActivityDataItem.Button(
                value = value,
                style = style.toButtonStyle(),
                action = action
            )
        }
    }
    ActivityViewItemDto.Unknown -> null
}
