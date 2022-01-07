package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey500
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier,
    icon: ImageResource = ImageResource.None,
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = Grey800,
        defaultBackgroundDarkColor = Grey800,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Grey500,
        disabledBackgroundDarkColor = Dark800,
        pressedBackgroundColor = Grey900,
        modifier = modifier,
        icon = icon,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, icon: ImageResource ->
            FixedSizeButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha
            )
        },
    )
}

@Preview(name = "Default", group = "Secondary button")
@Composable
private fun SecondaryButtonPreview() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Secondary button")
@Composable
private fun SecondaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Secondary button")
@Composable
private fun SecondaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecondaryButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            SecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
