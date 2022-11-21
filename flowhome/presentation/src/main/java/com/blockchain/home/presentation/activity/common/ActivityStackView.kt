package com.blockchain.home.presentation.activity.common

import androidx.compose.runtime.Composable
import com.blockchain.componentlib.tablerow.custom.ViewStyle
import com.blockchain.componentlib.tablerow.custom.ViewType
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.componentlib.utils.value

// styles - use domain ones instead to map
// text
enum class ActivityTextTypographyState {
    Paragraph2, Caption1
}

@Composable
fun ActivityTextTypographyState.toComposable() = when (this) {
    ActivityTextTypographyState.Paragraph2 -> AppTheme.typography.paragraph2
    ActivityTextTypographyState.Caption1 -> AppTheme.typography.caption1
}

enum class ActivityTextColorState {
    Title, Muted, Success, Error, Warning
}

@Composable
fun ActivityTextColorState.toComposable() = when (this) {
    ActivityTextColorState.Title -> AppTheme.colors.title
    ActivityTextColorState.Muted -> AppTheme.colors.muted
    ActivityTextColorState.Success -> AppTheme.colors.success
    ActivityTextColorState.Error -> AppTheme.colors.error
    ActivityTextColorState.Warning -> AppTheme.colors.warning
}

data class ActivityTextStyleState(
    val typography: ActivityTextTypographyState,
    val color: ActivityTextColorState,
    val strikethrough: Boolean = false
)

// tag
enum class ActivityTagStyleState {
    Success, Info, Warning, Error
}

fun ActivityTagStyleState.toTagType() = when (this) {
    ActivityTagStyleState.Success -> TagType.Success()
    ActivityTagStyleState.Info -> TagType.InfoAlt()
    ActivityTagStyleState.Warning -> TagType.Warning()
    ActivityTagStyleState.Error -> TagType.Error()
}

// component
sealed interface ActivityStackView {
    data class Text(
        val value: TextValue,
        val style: ActivityTextStyleState
    ) : ActivityStackView

    data class Tag(
        val value: TextValue,
        val style: ActivityTagStyleState
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
