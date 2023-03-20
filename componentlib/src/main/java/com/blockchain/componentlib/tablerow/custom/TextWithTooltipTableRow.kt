package com.blockchain.componentlib.tablerow.custom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun TextWithTooltipTableRow(
    startText: String,
    endTitle: String = "",
    endSubtitle: String? = null,
    isTappable: Boolean = false,
    tooltipContent: @Composable () -> Unit = { }
) {

    var showTooltip by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
            .clickable(enabled = isTappable, onClick = { showTooltip = !showTooltip })
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (isTappable) {
            AnimatedVisibility(
                visible = showTooltip,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                tooltipContent()
            }
        }
    }
}

@Composable
fun TextWithTooltipTableRow(
    startText: String,
    endTitle: String = "",
    endSubtitle: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleText(
                text = startText,
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            Spacer(
                modifier = Modifier.size(
                    width = AppTheme.dimensions.tinySpacing,
                    height = AppTheme.dimensions.smallestSpacing
                )
            )

            Image(imageResource = ImageResource.Local(R.drawable.ic_question))

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
}

@Preview(showBackground = true)
@Composable
fun TextTableRowPreview() {
    AppTheme {
        TextWithTooltipTableRow(
            startText = "Start text",
            endTitle = "End title",
            endSubtitle = "End subtitle",
            isTappable = true,
            tooltipContent = {
                SimpleText(
                    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed euismod, nisl sit amet " +
                        "aliquam luctus, nunc nisl aliquam lorem, nec aliquam nisl nunc et nisl.",
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Start
                )
            }
        )
    }
}
