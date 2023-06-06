package com.blockchain.componentlib.tablerow

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tag.DefaultTag
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer

@Composable
fun ActionTableRow(
    title: String,
    titleIcon: ImageResource? = null,
    subtitle: String? = null,
    tag: String = "",
    icon: StackedIcon = StackedIcon.None,
    defaultIconSize: Dp = AppTheme.dimensions.standardSpacing,
    actionIcon: ImageResource.Local = Icons.ChevronRight,
    onClick: () -> Unit
) {
    ActionTableRow(
        title = title,
        subtitle = subtitle,
        tag = tag,
        titleIcon = titleIcon,
        contentStart = {
            CustomStackedIcon(
                icon = icon,
                size = defaultIconSize
            )
        },
        actionIcon = actionIcon,
        onClick = onClick
    )
}

@Composable
private fun ActionTableRow(
    title: String,
    subtitle: String? = null,
    tag: String = "",
    titleIcon: ImageResource?,
    contentStart: @Composable (RowScope.() -> Unit)? = null,
    actionIcon: ImageResource.Local = Icons.ChevronRight,
    onClick: () -> Unit
) {
    TableRow(
        contentStart = contentStart,
        onContentClicked = onClick,
        content = {
            SmallHorizontalSpacer()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SimpleText(
                            text = title,
                            style = ComposeTypographies.Paragraph2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )
                        titleIcon?.let {
                            Image(
                                modifier = Modifier.padding(start = AppTheme.dimensions.smallestSpacing),
                                imageResource = titleIcon
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.size(
                            if (tag.isNotEmpty()) {
                                AppTheme.dimensions.composeSmallestSpacing
                            } else if (!subtitle.isNullOrBlank()) {
                                AppTheme.dimensions.smallestSpacing
                            } else 0.dp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = AppTheme.typography.caption1,
                                color = AppTheme.colors.body
                            )
                        }

                        if (!subtitle.isNullOrBlank() && tag.isNotEmpty()) {
                            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
                        }

                        if (tag.isNotEmpty()) {
                            DefaultTag(text = tag)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1F))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End
                ) {
                    Image(actionIcon.withTint(AppColors.muted))
                }
            }
        }
    )
}

@Preview
@Composable
private fun ActionTableRow() {
    ActionTableRow(
        title = "Bitcoin",
        subtitle = "BTC",
        onClick = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BalanceLockedTableRowDark() {
    ActionTableRow()
}
