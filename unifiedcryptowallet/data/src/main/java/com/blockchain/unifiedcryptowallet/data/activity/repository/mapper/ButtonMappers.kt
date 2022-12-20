package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityViewItemDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonAction.ActivityButtonActionType
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityButtonStyle

internal fun String.toButtonStyle(): ActivityButtonStyle {
    val primary = "primary"
    val secondary = "secondary"
    val tertiary = "tertiary"

    return when (this) {
        primary -> ActivityButtonStyle.Primary
        secondary -> ActivityButtonStyle.Tertiary // backend returns secondary for clear button so need to reverse them
        tertiary -> ActivityButtonStyle.Secondary
        else -> ActivityButtonStyle.Primary // todo what's the default here
    }
}

internal fun ActivityViewItemDto.Button.toButtonAction(): ActivityButtonAction? {
    val copy = "COPY"
    val openUrl = "OPEN_URL"

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
