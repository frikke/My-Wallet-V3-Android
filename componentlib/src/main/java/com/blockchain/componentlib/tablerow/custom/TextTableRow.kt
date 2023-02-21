package com.blockchain.componentlib.tablerow.custom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TextTableRow(
    startText: String,
    endTitle: String = "",
    endSubtitle: String? = null,
    onClick: () -> Unit = { },
    isTappable: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
            .clickable(enabled = isTappable, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleText(
            text = startText,
            style = ComposeTypographies.Paragraph2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )

        if (isTappable) {
            Spacer(
                modifier = Modifier.size(
                    width = AppTheme.dimensions.tinySpacing,
                    height = AppTheme.dimensions.smallestSpacing
                )
            )

            Image(imageResource = ImageResource.Local(R.drawable.ic_question))
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Column(horizontalAlignment = Alignment.End) {
            SimpleText(
                text = endTitle,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.End
            )

            endSubtitle?.let {
                SimpleText(
                    text = endSubtitle,
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.End
                )
            }
        }
    }
}

@Preview
@Composable
fun TextTableRowPreview() {
    AppTheme {
        TextTableRow(
            startText = "Start text",
            endTitle = "End title",
            endSubtitle = "End subtitle",
            onClick = {},
            isTappable = true
        )
    }
}
