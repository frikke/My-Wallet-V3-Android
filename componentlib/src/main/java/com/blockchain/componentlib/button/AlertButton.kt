package com.blockchain.componentlib.button

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey900

@Composable
fun AlertButton(
    text: String,
    onClick: () -> Unit,
    state: ButtonState,
    modifier: Modifier = Modifier
) {
    Button(
        text = text,
        onClick = onClick,
        state = state,
        shape = AppTheme.shapes.extraLarge,
        defaultTextColor = Color.White,
        defaultBackgroundLightColor = Grey900,
        defaultBackgroundDarkColor = Dark800,
        disabledBackgroundLightColor = Grey900,
        disabledBackgroundDarkColor = Dark800,
        pressedBackgroundColor = Color.Black,
        modifier = modifier.requiredHeightIn(min = 48.dp),
        buttonContent = { state: ButtonState, text: String, color: Color, _: Float, _: ImageResource ->
            Box {
                if (state == ButtonState.Loading) {
                    ButtonLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        loadingIconResId = R.drawable.ic_loading
                    )
                }

                val alpha = if (state == ButtonState.Loading) 0f else 1f
                Row(
                    Modifier.alpha(alpha),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_alert),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        color = color,
                        style = AppTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    )
}

@Preview(name = "Default", group = "Alert Button")
@Composable
private fun AlertButtonPreview() {
    AppTheme {
        AppSurface {
            AlertButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Alert Button")
@Preview
@Composable
private fun AlertButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            AlertButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertButtonPreview_Dark() {
    AppTheme {
        AppSurface {
            AlertButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AlertButtonLoadingPreview_Dark() {
    AppTheme {
        AppSurface {
            AlertButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
