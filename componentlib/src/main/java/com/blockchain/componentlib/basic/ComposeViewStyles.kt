package com.blockchain.componentlib.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.blockchain.componentlib.theme.AppTheme

enum class ComposeColors {
    Title,
    Body,
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
    Title4,
    Subheading,
    BodyMono,
    BodySlashedZero,
    Body1,
    Body2,
    ParagraphMono,
    Paragraph1,
    Paragraph2,
    Paragraph2SlashedZero,
    Caption1,
    Caption2,
    Overline,
    Micro1,
    Title2Mono,
    Title2SlashedZero,
    Micro2;

    @Composable
    fun toComposeTypography() =
        when (this) {
            Display -> AppTheme.typography.display
            Title1 -> AppTheme.typography.title1
            Title2 -> AppTheme.typography.title2
            Title3 -> AppTheme.typography.title3
            Title4 -> AppTheme.typography.title4
            Subheading -> AppTheme.typography.subheading
            BodyMono -> AppTheme.typography.bodyMono
            BodySlashedZero -> AppTheme.typography.bodySlashedZero
            Body1 -> AppTheme.typography.body1
            Body2 -> AppTheme.typography.body2
            ParagraphMono -> AppTheme.typography.paragraphMono
            Paragraph1 -> AppTheme.typography.paragraph1
            Paragraph2 -> AppTheme.typography.paragraph2
            Paragraph2SlashedZero -> AppTheme.typography.paragraph2SlashedZero
            Caption1 -> AppTheme.typography.caption1
            Caption2 -> AppTheme.typography.caption2
            Overline -> AppTheme.typography.overline
            Micro1 -> AppTheme.typography.micro1
            Micro2 -> AppTheme.typography.micro2
            Title2Mono -> AppTheme.typography.title2Mono
            Title2SlashedZero -> AppTheme.typography.title2SlashedZero
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

    fun toTextAlignment(): TextAlign =
        when (this) {
            Start -> TextAlign.Start
            Centre -> TextAlign.Center
            End -> TextAlign.End
        }
}
