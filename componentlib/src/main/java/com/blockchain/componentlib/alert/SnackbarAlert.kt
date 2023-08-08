package com.blockchain.componentlib.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Error
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

private val bgColorLight = Color(0XFF20242C)
private val bgColorDark = Color(0XFF000000)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

@Composable
fun SnackbarAlert(
    message: String,
    actionLabel: String = "",
    onActionClicked: () -> Unit = {},
    type: SnackbarType = SnackbarType.Info
) {

    val icon = when (type) {
        SnackbarType.Success -> Icons.Filled.Check.withTint(AppColors.success)
        SnackbarType.Error -> Icons.Filled.Alert.withTint(AppColors.error)
        SnackbarType.Warning -> Icons.Filled.Error.withTint(AppColors.warning)
        SnackbarType.Info -> null
    }

    val textColour = when (type) {
        SnackbarType.Success -> AppColors.success
        SnackbarType.Error -> AppColors.error
        SnackbarType.Warning -> AppColors.warning
        SnackbarType.Info -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            // prevents click throughs to views underneath the snack bar
            .clickable(true, onClick = {})
            .padding(horizontal = AppTheme.dimensions.smallSpacing, vertical = 14.dp)
    ) {
        icon?.let {
            Image(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(
                        end = AppTheme.dimensions.verySmallSpacing
                    ),
                imageResource = icon
            )
        }

        Text(
            text = message,
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .weight(1f),
            style = AppTheme.typography.paragraph1,
            textAlign = TextAlign.Start,
            color = textColour
        )

        if (actionLabel.isNotEmpty()) {
            Text(
                text = actionLabel.toUpperCase(Locale.current),
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(0.3f)
                    .clickable {
                        onActionClicked()
                    },
                style = AppTheme.typography.paragraph2,
                textAlign = TextAlign.End,
                color = AppColors.primary
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_noAction() {
    AppTheme {
        AppSurface {
            SnackbarAlert(message = "Snackbar message")
        }
    }
}

@Preview
@Composable
fun Snackbar_withAction() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                actionLabel = "action"
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_withAction_LongMessage() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar messageSnackbar messageSnackbar message" +
                    "Snackbar messageSnackbar messageSnackbar messageSnackbar messageSnackbar" +
                    " message",
                actionLabel = "action"
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_error_withAction() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                actionLabel = "action",
                type = SnackbarType.Error
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_success_withAction() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                actionLabel = "action",
                type = SnackbarType.Success
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_warning_withAction() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                actionLabel = "action",
                type = SnackbarType.Warning
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_error() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                type = SnackbarType.Error
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_success() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                type = SnackbarType.Success
            )
        }
    }
}

@Preview
@Composable
fun Snackbar_warning() {
    AppTheme {
        AppSurface {
            SnackbarAlert(
                message = "Snackbar message",
                type = SnackbarType.Warning
            )
        }
    }
}
