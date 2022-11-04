package com.blockchain.unifiedcryptowallet.domain.activity.model

// text
enum class ActivityTextTypography {
    Paragraph2, Caption1
}

enum class ActivityTextColor {
    Title, Muted, Success, Error, Warning
}

data class ActivityTextStyle(
    val typography: ActivityTextTypography,
    val color: ActivityTextColor,
    val strikethrough: Boolean
)

// tag
enum class ActivityTagStyle {
    Success, Warning
}

// component
sealed interface StackComponent {
    data class Text(
        val value: String,
        val style: ActivityTextStyle
    ) : StackComponent

    data class Tag(
        val value: String,
        val style: ActivityTagStyle
    ) : StackComponent
}
