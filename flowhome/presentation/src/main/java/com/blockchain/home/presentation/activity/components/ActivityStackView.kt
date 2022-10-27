package com.blockchain.home.presentation.activity.components

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.tablerow.generic.ViewStyle
import com.blockchain.componentlib.tablerow.generic.ViewType
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.theme.AppTheme

// styles - use domain ones instead to map
// text
enum class ActivityTextTypography {
    Paragraph2, Caption1
}

@Composable
fun ActivityTextTypography.toComposable() = when (this) {
    ActivityTextTypography.Paragraph2 -> AppTheme.typography.paragraph2
    ActivityTextTypography.Caption1 -> AppTheme.typography.caption1
}

enum class ActivityTextColor {
    Title, Muted, Success, Error, Warning
}

@Composable
fun ActivityTextColor.toComposable() = when (this) {
    ActivityTextColor.Title -> AppTheme.colors.title
    ActivityTextColor.Muted -> AppTheme.colors.muted
    ActivityTextColor.Success -> AppTheme.colors.success
    ActivityTextColor.Error -> AppTheme.colors.error
    ActivityTextColor.Warning -> AppTheme.colors.warning
}

data class ActivityTextStyle(
    val typography: ActivityTextTypography,
    val color: ActivityTextColor,
    val strikethrough: Boolean = false
)

// tag
enum class ActivityTagStyle {
    Success
}

fun ActivityTagStyle.toTagType() = when (this) {
    ActivityTagStyle.Success -> TagType.Success()
}

sealed interface ActivityStackViewComponent {
    val value: String

    data class Text(
        override val value: String,
        val style: ActivityTextStyle
    ) : ActivityStackViewComponent

    data class Tag(
        override val value: String,
        val style: ActivityTagStyle
    ) : ActivityStackViewComponent
}

@Composable
fun ActivityStackViewComponent.toViewType() = when (this) {
    is ActivityStackViewComponent.Tag -> {
        ViewType.Tag(
            value = value,
            style = style.toTagType()
        )
    }
    is ActivityStackViewComponent.Text -> {
        ViewType.Text(
            value = value,
            style = ViewStyle.TextStyle(
                style = style.typography.toComposable(),
                color = style.color.toComposable(),
                strikeThrough = style.strikethrough
            )
        )
    }
}

// main
data class ActivityStackView(
    val leadingImagePrimaryUrl: String?,
    val leadingImageImageSecondaryUrl: String?,
    val leading: List<ActivityStackViewComponent>,
    val trailing: List<ActivityStackViewComponent>,
)
