package com.blockchain.componentlib.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.conditional

@Composable
fun SmallInfoWithIcon(
    iconUrl: String?,
    text: String,
    trailingIcon: ImageResource = Icons.Filled.Info.copy(
        colorFilter = ColorFilter.tint(AppTheme.colors.muted)
    ),
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = AppColors.backgroundSecondary,
        shape = AppTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .conditional(onClick != null) {
                    clickable(onClick = onClick!!)
                }
                .padding(AppTheme.dimensions.tinySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconUrl?.let {
                Image(
                    ImageResource.Remote(
                        url = iconUrl,
                        shape = CircleShape,
                        size = AppTheme.dimensions.mediumSpacing
                    )
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            Text(
                modifier = Modifier.weight(1F),
                text = text,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.body
            )

            if (trailingIcon != ImageResource.None) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                Image(trailingIcon)
            }
        }
    }
}
