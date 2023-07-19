package com.blockchain.componentlib.alert

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppColors

@Composable
fun DefaultToastAlert(
    text: String,
    onClick: () -> Unit = {},
    startIcon: ImageResource = ImageResource.None
) {

    ToastAlert(
        text = text,
        startIcon = startIcon,
        backgroundColor = AppColors.alertBackground,
        iconColor = Color.White,
        onClick = onClick,
        textColor = Color.White
    )
}

@Preview
@Composable
fun DefaultToastAlert_Basic() {
    DefaultToastAlert(text = "Default")
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultToastAlert_Dark() {
    DefaultToastAlert(text = "Default", startIcon = ImageResource.Local(R.drawable.ic_chip_checkmark))
}
