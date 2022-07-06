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

@Composable
fun SmallMinimalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ButtonState = ButtonState.Enabled,
) {

    val contentPadding = PaddingValues(
        start = if (state == ButtonState.Loading) {
            dimensionResource(R.dimen.medium_margin)
        } else {
            dimensionResource(R.dimen.very_small_margin)
        },
        top = ButtonDefaults.ContentPadding.calculateTopPadding(),
        end = if (state == ButtonState.Loading) dimensionResource(R.dimen.medium_margin) else dimensionResource(
            R.dimen.very_small_margin
        ),
        bottom = ButtonDefaults.ContentPadding.calculateBottomPadding(),
    )

    OutlinedButton(
        text = text,
        onClick = onClick,
        shape = AppTheme.shapes.extraLarge,
        state = state,
        modifier = modifier.requiredHeightIn(min = dimensionResource(R.dimen.large_margin)),
        contentPadding = contentPadding,
        buttonContent = {
            state: ButtonState,
            text: String,
            textColor: Color,
            textAlpha: Float,
            loadingIconResId: Int,
            _: ImageResource, ->
            ButtonContentSmall(
                state = state,
                text = text,
                textColor = textColor,
                textAlpha = textAlpha,
                loadingIconResId = loadingIconResId,
            )
        },
    )
}

@Preview(name = "Default", group = "Small minimal button")
@Composable
private fun SmallMinimalButton_Basic() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button"
            )
        }
    }
}

@Preview(name = "Loading", group = "Small minimal button")
@Composable
private fun SmallMinimalButton_Loading() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small minimal button")
@Composable
private fun SmallMinimalButton_Disabled() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button",
                state = ButtonState.Disabled,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmallMinimalButton_DarkBasic() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button"
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmallMinimalButton_DarkLoading() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmallMinimalButton_DarkDisabled() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal Button",
                state = ButtonState.Disabled,
            )
        }
    }
}
