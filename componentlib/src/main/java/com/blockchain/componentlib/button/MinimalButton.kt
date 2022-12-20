package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun MinimalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ButtonState = ButtonState.Enabled,
    shape: Shape = AppTheme.shapes.extraLarge,
    icon: ImageResource = ImageResource.None,
    minHeight: Dp = 48.dp,
) {
    OutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier.requiredHeightIn(min = minHeight),
        state = state,
        shape = shape,
        icon = icon,
        buttonContent = {
                state: ButtonState,
                text: String,
                textColor: Color,
                textAlpha: Float,
                loadingIconResId: Int,
                icon: ImageResource,
            ->
            ButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha,
                loadingIconResId = loadingIconResId,
                icon = icon,
            )
        }
    )
}

@Preview(name = "Default", group = "Minimal button")
@Composable
private fun MinimalButton_Basic() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
            )
        }
    }
}

@Preview(name = "Loading", group = "Minimal button")
@Composable
private fun MinimalButton_Loading() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(name = "Disabled", group = "Minimal button")
@Composable
private fun MinimalButton_Disabled() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MinimalButton_DarkBasic() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button"
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MinimalButton_DarkLoading() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MinimalButton_DarkDisabled() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled,
            )
        }
    }
}
