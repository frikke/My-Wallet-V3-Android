package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.StackedIcons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun DefaultStackedIconTableRow(
    primaryText: String,
    secondaryText: String,
    topImageResource: ImageResource,
    bottomImageResource: ImageResource,
    onClick: () -> Unit,
) {
    TableRow(
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
                Text(
                    text = secondaryText,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }
        },
        contentEnd = {
            StackedIcons(
                topImageResource = topImageResource,
                bottomImageResource = bottomImageResource,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        },
        onContentClicked = onClick
    )
}

@Preview
@Composable
fun DefaultStackedIconTableRow_Basic() {
    AppTheme {
        AppSurface {
            DefaultStackedIconTableRow(
                primaryText = "Primary text",
                secondaryText = "Secondary text",
                topImageResource = ImageResource.Remote("", null),
                bottomImageResource = ImageResource.Remote("", null),
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
fun DefaultStackedIconTableRow_Basic_With_Icons() {
    AppTheme {
        AppSurface {
            DefaultStackedIconTableRow(
                primaryText = "Primary text",
                secondaryText = "Secondary text",
                topImageResource = ImageResource.Local(R.drawable.ic_blockchain, null),
                bottomImageResource = ImageResource.Local(R.drawable.ic_alert, null),
                onClick = {},
            )
        }
    }
}
