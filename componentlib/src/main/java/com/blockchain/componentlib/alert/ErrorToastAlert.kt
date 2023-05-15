package com.blockchain.componentlib.alert

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red600
import com.blockchain.componentlib.theme.White600

@Composable
fun ErrorToastAlert(
    text: String
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Dark800
    } else {
        Red600
    }

    val textColor = if (!isSystemInDarkTheme()) {
        Red400
    } else {
        Color.White
    }

    val iconColor = if (!isSystemInDarkTheme()) {
        Red600
    } else {
        White600
    }

    ToastAlert(
        text = text,
        startIcon = Icons.AlertOn,
        backgroundColor = backgroundColor,
        iconColor = Red600,
        textColor = textColor
    )
}

@Preview
@Composable
fun ErrorToastAlert_Basic() {
    AppTheme {
        AppSurface {
            ErrorToastAlert(text = "Error")
        }
    }
}

@Preview
@Composable
fun ErrorToastAlert_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            ErrorToastAlert(text = "Error")
        }
    }
}
