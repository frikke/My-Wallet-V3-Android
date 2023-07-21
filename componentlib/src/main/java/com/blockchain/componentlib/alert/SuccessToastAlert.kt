package com.blockchain.componentlib.alert

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppColors

@Composable
fun SuccessToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL
) {
    ToastAlert(
        text = text,
        startIconDrawableRes = startIconDrawableRes,
        backgroundColor = AppColors.alertBackground,
        iconColor = AppColors.success,
        textColor = AppColors.success
    )
}

@Preview
@Composable
fun SuccessToastAlert_Basic() {
    SuccessToastAlert(text = "Success")
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SuccessToastAlert_Dark() {
    SuccessToastAlert(text = "Success", startIconDrawableRes = R.drawable.ic_chip_checkmark)
}
