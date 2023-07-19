package com.blockchain.componentlib.alert

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun WarningToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
) {
    ToastAlert(
        text = text,
        startIconDrawableRes = startIconDrawableRes,
        backgroundColor = AppColors.alertBackground,
        iconColor = AppColors.warning,
        textColor = AppColors.warning
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
