package com.blockchain.componentlib.tablerow

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.icon.StackedIcons
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ActionStackedIconTableRow(
    primaryText: String,
    onClick: () -> Unit,
    iconTopUrl: String,
    iconBottomUrl: String,
    secondaryText: String? = null,
    @DrawableRes endIconResId: Int = R.drawable.ic_chevron_end,
) {
    TableRow(
        contentStart = {
            StackedIcons(
                iconTopUrl = iconTopUrl,
                iconBottomUrl = iconBottomUrl,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 16.dp)
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = primaryText,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body
                    )
                }
            }
        },
        contentEnd = {
            Image(
                painter = painterResource(id = endIconResId),
                contentDescription = null,
                modifier = Modifier.requiredSizeIn(
                    maxWidth = 24.dp,
                    maxHeight = 24.dp,
                ),
            )
        },
        onContentClicked = onClick
    )
}
