package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        defaultTextColor = Color.White,
        defaultBackgroundColor = Blue600,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Blue400,
        disabledBackgroundDarkColor = Grey900,
        pressedBackgroundColor = Blue700,
        modifier = modifier,
    )
}

@Preview
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

@Preview
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

@Preview
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
