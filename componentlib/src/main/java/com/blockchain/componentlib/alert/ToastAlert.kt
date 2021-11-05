package com.blockchain.componentlib.alert

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ToastAlert(
    text: String,
    @DrawableRes startIconDrawableRes: Int = ResourcesCompat.ID_NULL,
    backgroundColor: Color,
    iconColor: Color,
    textColor: Color
) {

    Row(
        Modifier
            .clip(AppTheme.shapes.large)
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {

        if (startIconDrawableRes != ResourcesCompat.ID_NULL) {
            Image(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(end = 8.dp),
                painter = painterResource(id = startIconDrawableRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconColor)
            )
        }

        Text(
            text = text,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            style = AppTheme.typography.body2,
            color = textColor
        )
    }
}
