package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue000
import com.blockchain.componentlib.theme.Blue600

@Composable
fun SmallTertiaryButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState = ButtonState.Enabled,
    modifier: Modifier = Modifier
) {
    val contentPadding = PaddingValues(
        start = if (state == ButtonState.Loading) {
            dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)
        } else {
            dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
        },
        top = ButtonDefaults.ContentPadding.calculateTopPadding(),
        end = if (state == ButtonState.Loading) {
            dimensionResource(com.blockchain.componentlib.R.dimen.medium_spacing)
        } else {
            dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
        },
        bottom = ButtonDefaults.ContentPadding.calculateBottomPadding()
    )

    Button(
        text = text,
        onClick = onClick,
        state = state,
        shape = AppTheme.shapes.extraLarge,
        defaultTextColor = Blue600,
        defaultBackgroundLightColor = Color.White,
        defaultBackgroundDarkColor = Color.White,
        disabledTextLightAlpha = 0.7f,
        disabledTextDarkAlpha = 0.4f,
        disabledBackgroundLightColor = Color.White,
        disabledBackgroundDarkColor = Color.White,
        pressedBackgroundColor = Blue000,
        modifier = modifier.requiredHeightIn(
            min = dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing)
        ),
        contentPadding = contentPadding,
        buttonContent = { state: ButtonState, text: String, textColor: Color, textAlpha: Float, _: ImageResource ->
            ButtonContentSmall(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha,
                loadingIconResId = R.drawable.ic_loading_minimal_light
            )
        }
    )
}

@Preview(name = "Default", group = "Small primary button")
@Composable
private fun SmallTertiaryButtonPreview() {
    AppTheme {
        AppSurface {
            SmallTertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small primary button")
@Composable
private fun SmallTertiaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            SmallTertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Small primary button")
@Composable
private fun SmallTertiaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            SmallTertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmallTertiaryButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            SmallTertiaryButton(
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
            SmallTertiaryButton(
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
            SmallTertiaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
