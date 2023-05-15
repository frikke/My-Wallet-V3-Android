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
import com.blockchain.componentlib.theme.Red000

@Composable
fun DestructiveMinimalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageResource = ImageResource.None
) {
    val textColor = when (state) {
        ButtonState.Enabled, ButtonState.Disabled -> AppTheme.colors.error
        ButtonState.Loading -> Color.Unspecified
    }

    OutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier.requiredHeightIn(min = 48.dp),
        state = state,
        icon = icon,
        pressedButtonLightColor = Red000,
        pressedBorderLightColor = textColor,
        pressedBorderDarkColor = textColor,
        defaultTextColor = textColor,
        defaultLoadingIconResId = R.drawable.ic_destructive_loading,
        buttonContent = {
                state: ButtonState,
                text: String,
                textColor: Color,
                textAlpha: Float,
                loadingIconResId: Int,
                icon: ImageResource
            ->
            ButtonContent(
                state = state,
                text = text,
                textColor = textColor,
                contentAlpha = textAlpha,
                loadingIconResId = loadingIconResId,
                icon = icon
            )
        }
    )
}

@Preview(name = "Default", group = "Destructive Minimal button")
@Composable
private fun DestructiveMinimalButton_Basic() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button"
            )
        }
    }
}

@Preview(name = "Loading", group = "Destructive Minimal button")
@Composable
private fun DestructiveMinimalButton_Loading() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(name = "Disabled", group = "Destructive Minimal button")
@Composable
private fun DestructiveMinimalButton_Disabled() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructiveMinimalButton_DarkBasic() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button"
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructiveMinimalButton_DarkLoading() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestructiveMinimalButton_DarkDisabled() {
    AppTheme {
        AppSurface {
            DestructiveMinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled
            )
        }
    }
}
