package com.blockchain.componentlib.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ButtonContent(
    state: ButtonState,
    text: String,
    textColor: Color,
    contentAlpha: Float,
    modifier: Modifier = Modifier,
    @DrawableRes loadingIconResId: Int = R.drawable.ic_loading,
    icon: ImageResource = ImageResource.None
) {
    Box(modifier) {
        if (state == ButtonState.Loading) {
            ButtonLoadingIndicator(
                modifier = Modifier.align(Alignment.Center),
                loadingIconResId = loadingIconResId
            )
        }

        Row(
            Modifier.alpha(contentAlpha),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (icon) {
                is ImageResource.Local -> {
                    Image(
                        imageResource = icon.withColorFilter(ColorFilter.tint(textColor)),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(AppTheme.dimensions.paddingSmall))
                }
                is ImageResource.LocalWithBackground -> {
                    Image(
                        imageResource = icon,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(AppTheme.dimensions.paddingSmall))
                }
                is ImageResource.Remote -> {
                    Image(
                        imageResource = icon,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(AppTheme.dimensions.paddingSmall))
                }
                ImageResource.None -> { /* no-op */
                }
            }
            Text(
                text = text,
                color = textColor,
                style = AppTheme.typography.body2
            )
        }
    }
}
