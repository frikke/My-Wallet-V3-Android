package com.blockchain.componentlib.switcher

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.alert.CircleIndicator
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Pending
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Stable
data class SwitcherItemIndicator(
    val size: Dp = 8.dp,
    val color: Color
)

@Composable
fun SwitcherItem(
    modifier: Modifier = Modifier,
    text: String,
    startIcon: ImageResource.Local? = null,
    endIcon: ImageResource.Local? = ImageResource.Local(
        id = R.drawable.ic_chevron_down,
        colorFilter = ColorFilter.tint(AppTheme.colors.dark)
    ),
    state: SwitcherState = SwitcherState.Enabled,
    indicator: SwitcherItemIndicator? = null,
    onClick: () -> Unit
) {
    val textColor = when (state) {
        SwitcherState.Enabled -> AppTheme.colors.title
        SwitcherState.Disabled -> AppTheme.colors.muted
    }

    val backgroundColor = when (state) {
        SwitcherState.Enabled -> AppTheme.colors.backgroundSecondary
        SwitcherState.Disabled -> AppTheme.colors.medium
    }

    Box {
        Surface(
            color = backgroundColor,
            shape = AppTheme.shapes.large
        ) {
            Row(
                modifier = modifier
                    .clickable {
                        onClick.invoke()
                    }
                    .padding(
                        vertical = AppTheme.dimensions.smallestSpacing,
                        horizontal = AppTheme.dimensions.smallSpacing
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                startIcon?.let {
                    Image(
                        imageResource = startIcon
                            .withSize(AppTheme.dimensions.smallSpacing)
                            .withTint(textColor)
                    )
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                }

                Text(
                    text = text,
                    style = AppTheme.typography.body1,
                    color = textColor
                )

                endIcon?.let {
                    Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                    Image(
                        imageResource = endIcon
                            .withSize(AppTheme.dimensions.smallSpacing)
                            .withTint(textColor)
                    )
                }
            }
        }

        indicator?.let {
            CircleIndicator(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .align(Alignment.TopEnd),
                size = indicator.size,
                color = indicator.color
            )
        }
    }
}

@Preview(name = "switcher item enabled light mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun Switcher_preview_enabled_light_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Enabled,
                startIcon = Icons.Pending,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { }
            )
        }
    }
}

@Preview(name = "switcher item disabled light mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun Switcher_preview_disabled_light_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Disabled,
                startIcon = Icons.Pending,
                endIcon = ImageResource.Local(
                    id = R.drawable.ic_arrow_right,
                    contentDescription = "IconArrowRight"
                ),
                indicator = SwitcherItemIndicator(
                    size = 8.dp,
                    color = Color.Red
                ),
                onClick = { }
            )
        }
    }
}

@Preview(name = "switcher item enabled dark mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Switcher_preview_enabled_dark_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Enabled,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { }
            )
        }
    }
}

@Preview(name = "switcher item disabled dark mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Switcher_preview_disabled_dark_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Disabled,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { }
            )
        }
    }
}
