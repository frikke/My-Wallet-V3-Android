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
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Blue700
import com.blockchain.componentlib.theme.Grey900

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier,
    defaultBackgroundColor: Color? = null,
    icon: ImageResource = ImageResource.None
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = defaultBackgroundColor ?: Blue600,
        defaultBackgroundDarkColor = defaultBackgroundColor ?: Blue600,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Blue400,
        disabledBackgroundDarkColor = Grey900,
        pressedBackgroundColor = Blue700,
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
private fun PrimaryButtonPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Primary button")
@Composable
private fun PrimaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Primary button")
@Composable
private fun PrimaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(name = "Button with image", group = "Primary button")
@Composable
private fun PrimaryButtonWithImagePreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
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
private fun PrimaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonDisabledPreview_Dark() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
