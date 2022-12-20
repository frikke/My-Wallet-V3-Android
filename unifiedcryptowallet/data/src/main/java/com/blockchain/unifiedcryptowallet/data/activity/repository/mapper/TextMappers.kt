package com.blockchain.unifiedcryptowallet.data.activity.repository.mapper

import com.blockchain.api.selfcustody.activity.ActivityTextStyleDto
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography

internal fun String.toTextTypography(): ActivityTextTypography {
    val display = "Display"
    val title1 = "Title 1"
    val title2 = "Title 2"
    val title3 = "Title 3"
    val subheading = "Subheading"
    val body1 = "Body 1"
    val body2 = "Body 2"
    val paragraph1 = "Paragraph 1"
    val paragraph2 = "Paragraph 2"
    val caption1 = "Caption 1"
    val caption2 = "Caption 2"
    val micro = "Micro (TabBar Text)"

    return when (this) {
        display -> ActivityTextTypography.Display
        title1 -> ActivityTextTypography.Title1
        title2 -> ActivityTextTypography.Title2
        title3 -> ActivityTextTypography.Title3
        subheading -> ActivityTextTypography.Subheading
        body1 -> ActivityTextTypography.Body1
        body2 -> ActivityTextTypography.Body2
        paragraph1 -> ActivityTextTypography.Paragraph1
        paragraph2 -> ActivityTextTypography.Paragraph2
        caption1 -> ActivityTextTypography.Caption1
        caption2 -> ActivityTextTypography.Caption2
        micro -> ActivityTextTypography.Micro
        else -> ActivityTextTypography.Paragraph2
    }
}

internal fun String.toTextColor(): ActivityTextColor {
    val title = "Title"
    val muted = "Body"
    val success = "Success"
    val error = "Error"
    val warning = "Warning"

    return when (this) {
        title -> ActivityTextColor.Title
        muted -> ActivityTextColor.Muted
        success -> ActivityTextColor.Success
        error -> ActivityTextColor.Error
        warning -> ActivityTextColor.Warning
        else -> ActivityTextColor.Title
    }
}

internal fun ActivityTextStyleDto.toTextStyle() = ActivityTextStyle(
    typography = typography.toTextTypography(),
    color = color.toTextColor(),
    strikethrough = strikethrough
)
