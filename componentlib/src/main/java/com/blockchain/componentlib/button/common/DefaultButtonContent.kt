package com.blockchain.componentlib.button.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Interest
import com.blockchain.componentlib.icons.Minus
import com.blockchain.componentlib.icons.Plus
import com.blockchain.componentlib.icons.Prices
import com.blockchain.componentlib.theme.AppTheme

@Stable
internal sealed interface ButtonStyle {
    @get:Composable
    val iconSize: Dp

    @get:Composable
    val textStyle: TextStyle

    @get:Composable
    val contentPadding: PaddingValues

    object Default : ButtonStyle {
        override val iconSize @Composable get() = AppTheme.dimensions.standardSpacing
        override val textStyle @Composable get() = AppTheme.typography.body2
        override val contentPadding
            @Composable get() = PaddingValues(
                horizontal = AppTheme.dimensions.smallSpacing,
                vertical = AppTheme.dimensions.verySmallSpacing
            )
    }

    object Small : ButtonStyle {
        override val iconSize @Composable get() = AppTheme.dimensions.smallSpacing
        override val textStyle @Composable get() = AppTheme.typography.paragraph2
        override val contentPadding
            @Composable get() = PaddingValues(
                horizontal = AppTheme.dimensions.verySmallSpacing,
                vertical = AppTheme.dimensions.tinySpacing
            )
    }
}

@Composable
internal fun DefaultButtonContent(
    state: ButtonState,
    style: ButtonStyle,
    text: String,
    textColor: Color,
    icon: ImageResource.Local? = null
) {
    Box {
        if (state == ButtonState.Loading) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.Center),
                size = style.iconSize,
                color = textColor
            )
        }

        val contentAlpha = when (state) {
            ButtonState.Enabled -> 1F
            ButtonState.Disabled -> 0.6F
            ButtonState.Loading -> 0F
        }

        Row(
            modifier = Modifier.alpha(contentAlpha),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Image(imageResource = icon.withSize(style.iconSize).withTint(textColor))

                if (text.isNotEmpty()) Spacer(Modifier.width(AppTheme.dimensions.tinySpacing))
            }
            Text(
                text = text,
                color = textColor,
                style = style.textStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDefaultButtonContent() {
    DefaultButtonContent(
        state = ButtonState.Enabled,
        style = ButtonStyle.Default,
        text = "Button Text",
        textColor = Color.Red,
        icon = null
    )
}

@Preview
@Composable
private fun PreviewDefaultButtonContentIcon() {
    DefaultButtonContent(
        state = ButtonState.Enabled,
        style = ButtonStyle.Default,
        text = "Button Text",
        textColor = Color.Red,
        icon = Icons.Plus
    )
}

@Preview
@Composable
private fun PreviewDefaultButtonContentSmall() {
    DefaultButtonContent(
        state = ButtonState.Enabled,
        style = ButtonStyle.Small,
        text = "Button Text",
        textColor = Color.Red,
        icon = Icons.Plus
    )
}

@Preview
@Composable
private fun PreviewDefaultButtonContentDisabled() {
    DefaultButtonContent(
        state = ButtonState.Disabled,
        style = ButtonStyle.Default,
        text = "Button Text",
        textColor = Color.Red,
        icon = Icons.Plus
    )
}

@Preview
@Composable
private fun PreviewDefaultButtonContentLoading() {
    DefaultButtonContent(
        state = ButtonState.Loading,
        style = ButtonStyle.Default,
        text = "Button Text",
        textColor = Color.Red,
        icon = Icons.Plus
    )
}
