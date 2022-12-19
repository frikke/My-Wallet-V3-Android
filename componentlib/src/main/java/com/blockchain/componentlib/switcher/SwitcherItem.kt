package com.blockchain.componentlib.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SwitcherItem(
    modifier: Modifier = Modifier,
    text: String,
    startIcon: ImageResource = ImageResource.None,
    endIcon: ImageResource = ImageResource.Local(
        id = R.drawable.ic_chevron_down,
        colorFilter = ColorFilter.tint(AppTheme.colors.dark)
    ),
    state: SwitcherState = SwitcherState.Enabled,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit,
) {

    val textColor = when (state) {
        SwitcherState.Enabled -> AppTheme.colors.title
        SwitcherState.Disabled -> AppTheme.colors.muted
    }

    val backgroundColor = when (state) {
        SwitcherState.Enabled -> AppTheme.colors.background
        SwitcherState.Disabled -> AppTheme.colors.medium
    }

    Row(
        modifier = modifier
            .clickable {
                onClick.invoke()
            }
            .background(
                backgroundColor,
                RoundedCornerShape(dimensionResource(id = R.dimen.medium_spacing))
            )
            .padding(dimensionResource(id = R.dimen.tiny_spacing)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (startIcon != ImageResource.None) {
            Image(
                imageResource = startIcon,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.tiny_spacing)
                    )
            )
        }
        Text(
            text = text,
            style = AppTheme.typography.body1,
            color = textColor,
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.tiny_spacing),
                    end = dimensionResource(id = R.dimen.tiny_spacing)
                )

        )
        Image(
            imageResource = endIcon
        )
    }
}
