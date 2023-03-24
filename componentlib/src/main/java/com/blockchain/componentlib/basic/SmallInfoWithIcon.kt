package com.blockchain.componentlib.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SmallInfoWithIcon(
    iconUrl: String?,
    text: String,
    trailingIcon: ImageResource = Icons.Filled.Info.copy(
        colorFilter = ColorFilter.tint(AppTheme.colors.dark)
    )
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
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
            color = AppTheme.colors.muted,
        )

        if (trailingIcon != ImageResource.None) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            Image(trailingIcon)
        }
    }
}
