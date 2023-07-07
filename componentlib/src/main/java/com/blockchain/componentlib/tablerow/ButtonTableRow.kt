package com.blockchain.componentlib.tablerow

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.SecondarySmallButton
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ButtonTableRow(
    title: String,
    subtitle: String,
    imageResource: ImageResource = ImageResource.None,
    defaultIconSize: Dp = AppTheme.dimensions.largeSpacing,
    actionText: String,
    onClick: () -> Unit
) {
    ButtonTableRow(
        title = title,
        subtitle = subtitle,
        icon = if (imageResource is ImageResource.None) {
            StackedIcon.None
        } else {
            StackedIcon.SingleIcon(imageResource)
        },
        defaultIconSize = defaultIconSize,
        actionText = actionText,
        onClick = onClick
    )
}

@Composable
fun ButtonTableRow(
    title: String,
    subtitle: String,
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.largeSpacing,
    actionText: String,
    onClick: () -> Unit
) {
    ButtonTableRow(
        title = title,
        subtitle = subtitle,
        contentStart = {
            CustomStackedIcon(
                icon = icon,
                size = defaultIconSize,
                iconBackground = AppColors.backgroundSecondary
            )
        },
        actionText = actionText,
        onClick = onClick
    )
}

@Composable
private fun ButtonTableRow(
    title: String,
    subtitle: String,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    actionText: String,
    onClick: () -> Unit
) {
    TableRow(
        contentStart = contentStart,
        content = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Column {
                Text(
                    text = title,
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.title
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

                Text(
                    text = subtitle,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
            }
        },
        contentEnd = {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            SecondarySmallButton(text = actionText, onClick = onClick)
        },
        onContentClicked = onClick
    )
}

@Preview
@Composable
fun PreviewButtonTableRow_ImageResource() {
    ButtonTableRow(
        title = "Automate your buys",
        subtitle = "Buy crypto daily, weekly, or monthly",
        imageResource = Icons.Filled.Sync.withTint(AppTheme.colors.primary),
        actionText = "GO",
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewButtonTableRow_ImageResourceDark() {
    PreviewButtonTableRow_ImageResource()
}
