package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.StackedIcons
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ActionStackedIconTableRow(
    primaryText: String,
    onClick: () -> Unit,
    topImageResource: ImageResource,
    bottomImageResource: ImageResource,
    secondaryText: String? = null,
    endImageResource: ImageResource = ImageResource.Local(
        id = R.drawable.ic_chevron_end,
        contentDescription = null
    ),
) {
    TableRow(
        contentStart = {
            StackedIcons(
                topImageResource = topImageResource,
                bottomImageResource = bottomImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = dimensionResource(R.dimen.medium_margin))
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
                imageResource = endImageResource,
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(R.dimen.standard_margin),
                    maxHeight = dimensionResource(R.dimen.standard_margin),
                ),
            )
        },
        onContentClicked = onClick
    )
}
