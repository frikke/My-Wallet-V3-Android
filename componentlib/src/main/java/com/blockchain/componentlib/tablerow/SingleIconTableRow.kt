package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SingleIconTableRow(
    primaryText: String,
    onClick: () -> Unit = {},
    imageResource: ImageResource.LocalWithBackground,
    secondaryText: String? = null,
    endImageResource: ImageResource = ImageResource.None,
    backgroundColor: Color = AppTheme.colors.background
) {
    TableRow(
        contentStart = {
            CustomStackedIcon(
                icon = StackedIcon.SingleIcon(imageResource),
                iconBackground = imageResource.backgroundColor
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.dimensions.smallSpacing)
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
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                )
            )
        },
        onContentClicked = onClick,
        backgroundColor = backgroundColor
    )
}

@Preview(showBackground = true)
@Composable
fun SingleIconTableRowPreview() {
    AppTheme {
        SingleIconTableRow(
            primaryText = "Primary Text",
            imageResource = ImageResource.LocalWithBackground(
                id = R.drawable.ic_blockchain,
                contentDescription = null,
                backgroundColor = AppTheme.colors.light,
                iconColor = AppTheme.colors.title
            ),
            secondaryText = "Secondary Text"
        )
    }
}
