package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityViewItemDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction.ActivityButtonActionType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle

internal fun String.toButtonStyle(): ActivityButtonStyle {
    val primary = "Primary"
    val secondary = "Secondary"
    val tertiary = "Tertiary"

    return when (this) {
        primary -> ActivityButtonStyle.Primary
        secondary -> ActivityButtonStyle.Secondary
        tertiary -> ActivityButtonStyle.Tertiary
        else -> ActivityButtonStyle.Primary // todo what's the default here
    }
}

internal fun ActivityViewItemDto.Button.toButtonAction(): ActivityButtonAction? {
    val copy = "Copy"
    val openUrl = "OpenUrl"

    val type = when (actionType) {
        copy -> ActivityButtonActionType.Copy
        openUrl -> ActivityButtonActionType.OpenUrl
        else -> null
    }

    return type?.let {
        ActivityButtonAction(
            type = type,
            data = actionData
        )
    }
}
