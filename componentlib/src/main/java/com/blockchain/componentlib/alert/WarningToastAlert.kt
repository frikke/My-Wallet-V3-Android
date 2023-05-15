package com.blockchain.componentlib.alert

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.theme.Orange400

@Composable
fun WarningToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
) {
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Dark800
    } else {
        Orange400
    }

    val textColor = if (!isSystemInDarkTheme()) {
        Orange400
    } else {
        Grey900
    }

    val iconColor = if (!isSystemInDarkTheme()) {
        Grey400
    } else {
        Dark600
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
fun WarningToastAlert_Basic() {
    AppTheme {
        AppSurface {
            WarningToastAlert(text = "Warning")
        }
    }
}

@Preview
@Composable
fun WarningToastAlert_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            WarningToastAlert(text = "Warning", startIconDrawableRes = R.drawable.ic_chip_checkmark)
        }
    }
}
