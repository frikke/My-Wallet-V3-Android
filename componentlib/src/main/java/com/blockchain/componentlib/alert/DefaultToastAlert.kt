package com.blockchain.componentlib.alert

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DefaultToastAlert(
    text: String,
    onClick: () -> Unit = {},
    startIcon: ImageResource = ImageResource.None
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Dark800
    } else {
        Grey300
    }

    val textColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Grey900
    }

    val iconColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Grey600
    }

    ToastAlert(
        text = text,
        startIcon = startIcon,
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        onClick = onClick,
        textColor = textColor
    )
}

@Preview
@Composable
fun DefaultToastAlert_Basic() {
    AppTheme {
        AppSurface {
            DefaultToastAlert(text = "Default")
        }
    }
}

@Preview
@Composable
fun DefaultToastAlert_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            DefaultToastAlert(text = "Default", startIcon = ImageResource.Local(R.drawable.ic_chip_checkmark))
        }
    }
}
