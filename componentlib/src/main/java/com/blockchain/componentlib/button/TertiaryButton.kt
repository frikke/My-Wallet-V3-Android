package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600

@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState = ButtonState.Enabled,
    modifier: Modifier = Modifier,
    defaultBackgroundColor: Color? = null,
    icon: ImageResource = ImageResource.None
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = Blue600,
        defaultBackgroundLightColor = defaultBackgroundColor ?: Color.White,
        defaultBackgroundDarkColor = defaultBackgroundColor ?: Color.White,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Color.White,
        disabledBackgroundDarkColor = Color.White,
        pressedBackgroundColor = Blue000,
        modifier = modifier.requiredHeightIn(min = 48.dp),
        icon = icon,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, icon: ImageResource ->
            ButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha,
                icon = icon,
            )
        },
    )
}

@Preview(name = "Default", group = "Primary button")
@Composable
private fun TertiaryButtonPreview() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Primary button")
@Composable
private fun TertiaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Primary button")
@Composable
private fun TertiaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(name = "Button with image", group = "Primary button")
@Composable
private fun TertiaryButtonWithImagePreview() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled,
                icon = ImageResource.Local(R.drawable.ic_blockchain)
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TertiaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TertiaryButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TertiaryButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            TertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
