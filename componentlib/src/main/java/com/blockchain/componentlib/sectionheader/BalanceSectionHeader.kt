package com.blockchain.componentlib.sectionheader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.NoRippleProvider

@Composable
fun BalanceSectionHeader(
    primaryText: String,
    secondaryText: String,
    iconResource: ImageResource = ImageResource.Local(R.drawable.ic_star, null),
    onIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(AppTheme.dimensions.paddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = primaryText,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                modifier = Modifier
            )
            Text(
                text = secondaryText,
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.body,
                modifier = Modifier
            )
        }
        CompositionLocalProvider(
            LocalRippleTheme provides NoRippleProvider
        ) {
            Image(
                modifier = Modifier.clickable(onClick = onIconClick),
                imageResource = iconResource
            )
        }
    }
}

@Preview
@Composable
private fun BalanceSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            BalanceSectionHeader(
                primaryText = "\$12,293.21",
                secondaryText = "0.1393819 BTC"
            )
        }
    }
}
