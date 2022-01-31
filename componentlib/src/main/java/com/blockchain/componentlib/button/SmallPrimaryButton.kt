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
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Blue700
import com.blockchain.componentlib.theme.Grey900

@Composable
fun SmallPrimaryButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier,
) {
    val contentPadding = PaddingValues(
        start = if (state == ButtonState.Loading) 16.dp else 12.dp,
        top = ButtonDefaults.ContentPadding.calculateTopPadding(),
        end = if (state == ButtonState.Loading) 16.dp else 12.dp,
        bottom = ButtonDefaults.ContentPadding.calculateBottomPadding(),
    )

    Button(
        text = text,
        onClick = onClick,
        state = state,
        shape = AppTheme.shapes.extraLarge,
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = Blue600,
        defaultBackgroundDarkColor = Blue600,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Blue400,
        disabledBackgroundDarkColor = Grey900,
        pressedBackgroundColor = Blue700,
        modifier = modifier.requiredHeightIn(min = 32.dp),
        contentPadding = contentPadding,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, _: ImageResource ->
            ButtonContentSmall(
                state = state,
                text = text,
                textColor = textColor,
                textAlpha = textAlpha
            )
        },
    )
}

@Preview(name = "Default", group = "Small primary button")
@Composable
private fun SmallPrimaryButtonPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small primary button")
@Composable
private fun SmallPrimaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Small primary button")
@Composable
private fun SmallPrimaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmallPrimaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
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
            SmallPrimaryButton(
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
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
