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
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Green600
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.White600

@Composable
fun SuccessToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
) {

    val backgroundColor = if (!isSystemInDarkTheme()) {
        Dark800
    } else {
        Green600
    }

    val textColor = if (!isSystemInDarkTheme()) {
        Green400
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
fun SuccessToastAlert_Basic() {
    AppTheme {
        AppSurface {
            SuccessToastAlert(text = "Success")
        }
    }
}

@Preview
@Composable
fun SuccessToastAlert_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            SuccessToastAlert(text = "Success", startIconDrawableRes = R.drawable.ic_chip_checkmark)
        }
    }
}
