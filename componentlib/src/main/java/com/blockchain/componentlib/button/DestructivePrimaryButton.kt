package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red700
import com.blockchain.componentlib.theme.Red900

@Composable
fun DestructivePrimaryButton(
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
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = defaultBackgroundColor ?: AppTheme.colors.error,
        defaultBackgroundDarkColor = defaultBackgroundColor ?: AppTheme.colors.error,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Red400,
        disabledBackgroundDarkColor = Red900,
        pressedBackgroundColor = Red700,
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

@Preview(name = "Default", group = "Destructive button")
@Composable
private fun DestructivePrimaryButtonPreview() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Destructive Primary button")
@Composable
private fun DestructivePrimaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Destructive Primary button")
@Composable
private fun DestructivePrimaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructivePrimaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructivePrimaryButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructivePrimaryButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            DestructivePrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
