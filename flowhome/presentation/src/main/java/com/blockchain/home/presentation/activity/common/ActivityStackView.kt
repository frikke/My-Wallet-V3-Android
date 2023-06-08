package com.blockchain.home.presentation.activity.common

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.tablerow.custom.ViewStyle
import com.blockchain.componentlib.tablerow.custom.ViewType
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTagStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextColor
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextStyle
import com.blockchain.unifiedcryptowallet.domain.activity.model.ActivityTextTypography

@Composable
fun ActivityTextTypography.toComposable() = when (this) {
    ActivityTextTypography.Display -> AppTheme.typography.display
    ActivityTextTypography.Title1 -> AppTheme.typography.title1
    ActivityTextTypography.Title2 -> AppTheme.typography.title2
    ActivityTextTypography.Title3 -> AppTheme.typography.title3
    ActivityTextTypography.Subheading -> AppTheme.typography.subheading
    ActivityTextTypography.Body1 -> AppTheme.typography.body1
    ActivityTextTypography.Body2 -> AppTheme.typography.body2
    ActivityTextTypography.Paragraph1 -> AppTheme.typography.paragraph1
    ActivityTextTypography.Paragraph2 -> AppTheme.typography.paragraph2
    ActivityTextTypography.Caption1 -> AppTheme.typography.caption1
    ActivityTextTypography.Caption2 -> AppTheme.typography.caption2
    ActivityTextTypography.Micro -> AppTheme.typography.micro1
}

@Composable
fun ActivityTextColor.toComposable() = when (this) {
    ActivityTextColor.Title -> AppTheme.colors.title
    ActivityTextColor.Muted -> AppTheme.colors.muted
    ActivityTextColor.Success -> AppTheme.colors.success
    ActivityTextColor.Error -> AppTheme.colors.error
    ActivityTextColor.Warning -> AppTheme.colors.warning
}

// tag
fun ActivityTagStyle.toTagType() = when (this) {
    ActivityTagStyle.Default -> TagType.Default()
    ActivityTagStyle.Success -> TagType.Success()
    ActivityTagStyle.Info -> TagType.InfoAlt()
    ActivityTagStyle.Warning -> TagType.Warning()
    ActivityTagStyle.Error -> TagType.Error()
}

// component
sealed interface ActivityStackView {
    data class Text(
        val value: TextValue,
        val style: ActivityTextStyle
    ) : ActivityStackView

    data class Tag(
        val value: TextValue,
        val style: ActivityTagStyle
    ) : ActivityStackView
}

@Composable
fun ActivityStackView.toViewType() = when (this) {
    is ActivityStackView.Tag -> {
        ViewType.Tag(
            value = value.value(),
            style = style.toTagType()
        )
    }

    is ActivityStackView.Text -> {
        ViewType.Text(
            value = value.value(),
            style = ViewStyle.TextStyle(
                style = style.typography.toComposable(),
                color = style.color.toComposable(),
                strikeThrough = style.strikethrough
            )
        )
    }
}
