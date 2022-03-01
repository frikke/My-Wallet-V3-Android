package com.blockchain.componentlib.alert

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ToastAlert(
    text: String,
    // TODO(antonis-bc): AND-5826 Remove any DrawableRes from component library
    @DrawableRes
    startIconDrawableRes: Int = ResourcesCompat.ID_NULL,
    startIcon: ImageResource = ImageResource.None,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit = {},
    textColor: Color
) {

    Row(
        modifier = Modifier
            .clip(AppTheme.shapes.extraLarge)
            .background(backgroundColor)
            .wrapContentWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.standard_margin),
                vertical = dimensionResource(R.dimen.very_small_margin)
            )
    ) {
        val composeImage =
            (startIcon as? ImageResource.Local)?.withColorFilter(ColorFilter.tint(iconColor)) ?: startIcon
        if (startIcon != ImageResource.None) {
            Image(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(end = 8.dp),
                imageResource = composeImage
            )
        }

        Text(
            text = text,
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .clickable {
                    onClick()
                },
            style = AppTheme.typography.body2,
            color = textColor
        )
    }
}
