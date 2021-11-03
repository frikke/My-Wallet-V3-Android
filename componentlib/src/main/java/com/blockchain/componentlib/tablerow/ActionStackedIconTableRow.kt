package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
    iconButtomUrl: String,
    secondaryText: String? = null,
) {
    TableRow(
        contentStart = {
            StackedIcons(
                iconTopUrl = iconTopUrl,
                iconButtomUrl = iconButtomUrl,
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
                    color = AppTheme.colors.title,
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = AppTheme.typography.paragraph1,
                        color = AppTheme.colors.body,
                    )
                }
            }
        },
        contentEnd = {
            Image(
                painter = painterResource(id = R.drawable.ic_chevron_end),
                contentDescription = null
            )
        },
        onContentClicked = onClick,
    )
}