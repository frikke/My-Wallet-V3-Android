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
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DefaultToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
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
        Grey400
    } else {
        Grey600
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
            DefaultToastAlert(text = "Default", startIconDrawableRes = R.drawable.ic_chip_checkmark)
        }
    }
}
