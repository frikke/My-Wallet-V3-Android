package com.blockchain.componentlib.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Star
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800

// todo(othman) customize it with types
@Composable
fun PillAlert(
    modifier: Modifier = Modifier,
    message: String,
    icon: ImageResource = ImageResource.None,
    backgroundColor: Color = Dark800,
    textColor: Color = AppTheme.colors.background
) {
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = AppTheme.shapes.extraLarge
            )
            .padding(
                horizontal = AppTheme.dimensions.standardSpacing,
                vertical = AppTheme.dimensions.verySmallSpacing
            )
            .clickable(true, onClick = {}) ,
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (icon != ImageResource.None) {
            Image(
                modifier = Modifier.size(AppTheme.dimensions.smallSpacing),
                imageResource = icon
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }

        Text(
            text = message,
            style = AppTheme.typography.body2,
            color = textColor
        )
    }
}

@Preview
@Composable
fun PreviewPillAlert() {
    AppTheme {
        AppSurface {
            PillAlert(message = "Added to favorites", icon = Icons.Filled.Star.withTint(Color(0XFFFFCD53)))
        }
    }
}
