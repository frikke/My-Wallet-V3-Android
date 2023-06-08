package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000

@Composable
fun MinimalButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    state: ButtonState = ButtonState.Enabled,
    textColor: Color = AppColors.primary,
    defaultBackgroundColor: Color? = null,
    icon: ImageResource = ImageResource.None,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = textColor,
        defaultBackgroundLightColor = defaultBackgroundColor ?: AppTheme.colors.backgroundSecondary,
        defaultBackgroundDarkColor = defaultBackgroundColor ?: AppTheme.colors.backgroundSecondary,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = AppTheme.colors.backgroundSecondary,
        disabledBackgroundDarkColor = AppTheme.colors.backgroundSecondary,
        pressedBackgroundColor = Blue000,
        modifier = modifier.requiredHeightIn(min = 48.dp),
        icon = icon,
        contentPadding = contentPadding,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, icon: ImageResource ->
            ButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha,
                icon = icon
            )
        }
    )
}

@Preview(name = "Default", group = "Primary button")
@Composable
private fun TertiaryButtonPreview() {
    AppTheme {
        AppSurface {
            MinimalButton(
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
            MinimalButton(
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
            MinimalButton(
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
            MinimalButton(
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
            MinimalButton(
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
            MinimalButton(
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
            MinimalButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
