package com.blockchain.componentlib.alert

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Red400
import com.blockchain.componentlib.theme.Red600
import com.blockchain.componentlib.theme.White600

@Composable
fun ErrorToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
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
        Grey400
    } else {
        White600
    }

    ToastAlert(
        text = text,
        startIconDrawableRes = startIconDrawableRes,
        backgroundColor = backgroundColor,
        iconColor = iconColor,
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
            ErrorToastAlert(text = "Error", startIconDrawableRes = R.drawable.ic_chip_checkmark)
        }
    }
}
