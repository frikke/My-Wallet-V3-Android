package com.blockchain.componentlib.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppTheme

enum class ComposeColors {
    Title,
    Body,
    Overlay,
    Muted,
    Dark,
    Medium,
    Light,
    Primary,
    Success,
    Warning,
    Error;

    @Composable
    fun toComposeColor(): Color =
        when (this) {
            Title -> AppTheme.colors.title
            Body -> AppTheme.colors.body
            Overlay -> AppTheme.colors.overlay
            Muted -> AppTheme.colors.muted
            Dark -> AppTheme.colors.dark
            Medium -> AppTheme.colors.medium
            Light -> AppTheme.colors.light
            Primary -> AppTheme.colors.primary
            Success -> AppTheme.colors.success
            Warning -> AppTheme.colors.warning
            Error -> AppTheme.colors.error
        }
}

enum class ComposeTypographies {
    Display,
    Title1,
    Title2,
    Title3,
    Subheading,
    BodyMono,
    Body1,
    Body2,
    ParagraphMono,
    Paragraph1,
    Paragraph2,
    Caption1,
    Caption2,
    Overline,
    Micro;

    @Composable
    fun toComposeTypography() =
        when (this) {
            Display -> AppTheme.typography.display
            Title1 -> AppTheme.typography.title1
            Title2 -> AppTheme.typography.title2
            Title3 -> AppTheme.typography.title3
            Subheading -> AppTheme.typography.subheading
            BodyMono -> AppTheme.typography.bodyMono
            Body1 -> AppTheme.typography.body1
            Body2 -> AppTheme.typography.body2
            ParagraphMono -> AppTheme.typography.paragraphMono
            Paragraph1 -> AppTheme.typography.paragraph1
            Paragraph2 -> AppTheme.typography.paragraph2
            Caption1 -> AppTheme.typography.caption1
            Caption2 -> AppTheme.typography.caption2
            Overline -> AppTheme.typography.overline
            Micro -> AppTheme.typography.micro
        }
}

enum class ComposeGravities {
    Start,
    Centre,
    End;

    @Composable
    fun toComposeGravity(): Alignment.Horizontal =
        when (this) {
            Start -> Alignment.Start
            Centre -> Alignment.CenterHorizontally
            End -> Alignment.End
        }
}
