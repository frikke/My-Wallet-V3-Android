package com.blockchain.componentlib.alert

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.icons.AlertOn
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppColors

@Composable
fun ErrorToastAlert(
    text: String
) {
    ToastAlert(
        text = text,
        startIcon = Icons.AlertOn,
        backgroundColor = AppColors.alertBackground,
        iconColor = AppColors.error,
        textColor = AppColors.error
    )
}

@Preview
@Composable
fun ErrorToastAlert_Basic() {
    ErrorToastAlert(text = "Error")
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ErrorToastAlert_Dark() {
    ErrorToastAlert(text = "Error")
}
