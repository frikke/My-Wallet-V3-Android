package com.blockchain.unifiedcryptowallet.domain.activity.model

// text
enum class ActivityTextTypography {
    Display,
    Title1, Title2, Title3,
    Subheading,
    Body1, Body2,
    Paragraph1, Paragraph2,
    Caption1, Caption2,
    Micro
}

enum class ActivityTextColor {
    Title, Muted, Success, Error, Warning
}

data class ActivityTextStyle(
    val typography: ActivityTextTypography,
    val color: ActivityTextColor,
    val strikethrough: Boolean = false
)

// tag
enum class ActivityTagStyle {
    Default, Success, Info, Warning, Error
}

// component
sealed interface StackComponent {
    val value: String

    data class Text(
        override val value: String,
        val style: ActivityTextStyle
    ) : StackComponent

    data class Tag(
        override val value: String,
        val style: ActivityTagStyle
    ) : StackComponent
}
