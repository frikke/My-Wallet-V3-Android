package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityTextStyleDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography

internal fun String.toTextTypography(): ActivityTextTypography {
    val paragraph2 = "Paragraph2"
    val caption1 = "Caption1"

    return when (this) {
        paragraph2 -> ActivityTextTypography.Paragraph2
        caption1 -> ActivityTextTypography.Caption1
        else -> ActivityTextTypography.Paragraph2 // todo what's the default here
    }
}

internal fun String.toTextColor(): ActivityTextColor {
    val title = "Title"
    val muted = "Muted"
    val success = "Success"
    val error = "Error"
    val warning = "Warning"

    return when (this) {
        title -> ActivityTextColor.Title
        muted -> ActivityTextColor.Muted
        success -> ActivityTextColor.Success
        error -> ActivityTextColor.Error
        warning -> ActivityTextColor.Warning
        else -> ActivityTextColor.Title // todo what's the default here
    }
}

internal fun ActivityTextStyleDto.toTextStyle() = ActivityTextStyle(
    typography = style.toTextTypography(),
    color = color.toTextColor(),
    strikethrough = strikethrough
)
