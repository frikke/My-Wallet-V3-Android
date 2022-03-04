package com.blockchain.componentlib.alert

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.alert.abstract.SnackbarType
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Orange400
import com.blockchain.componentlib.theme.Red400

@Composable
fun SnackbarAlert(
    message: String,
    actionLabel: String = "",
    onActionClicked: () -> Unit = {},
    type: SnackbarType = SnackbarType.Info
) {

    val backgroundColour = if (!isSystemInDarkTheme()) {
        Dark800
    } else {
        Color.Black
    }

    val icon = when (type) {
        SnackbarType.Success -> R.drawable.ic_success
        SnackbarType.Error -> R.drawable.ic_error
        SnackbarType.Warning -> R.drawable.ic_warning
        SnackbarType.Info -> null
    }

    val textColour = when (type) {
        SnackbarType.Success -> Green400
        SnackbarType.Error -> Red400
        SnackbarType.Warning -> Orange400
        SnackbarType.Info -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColour)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = if (icon != null) {
            Arrangement.Start
        } else {
            Arrangement.SpaceBetween
        }
    ) {

        icon?.let {
            Image(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(end = 16.dp),
                painter = painterResource(id = icon),
                contentDescription = null
            )
        }

        Text(
            text = message,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            style = AppTheme.typography.paragraph1,
            color = textColour
        )

        if (actionLabel.isNotEmpty()) {
            Spacer(
                modifier = Modifier.weight(1f)
            )
            Text(
                text = actionLabel.toUpperCase(Locale.current),
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(start = 24.dp)
                    .clickable {
                        onActionClicked()
                    },
                style = AppTheme.typography.paragraph2,
                color = Blue400,
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
