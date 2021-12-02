package com.blockchain.componentlib.tablerow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.charts.SparkLine
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun BalanceTableRowLarge(
    title: String,
    historicalRates: List<SparkLineHistoricalRate>,
    primaryBylineStart: AnnotatedString,
    primaryBylineEnd: AnnotatedString,
    secondaryBylineStart: AnnotatedString,
    secondaryBylineEnd: AnnotatedString,
    startImageResource: ImageResource,
    onClick: (() -> Unit)? = null,
) {
    TableRow(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row {
                    Text(
                        text = title,
                        style = AppTheme.typography.body2,
                        modifier = Modifier.weight(1f),
                        color = AppTheme.colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    SparkLine(
                        historicalRates = historicalRates,
                        modifier = Modifier.size(64.dp, 16.dp),
                    )
                }
                TableRowText(
                    startText = primaryBylineStart,
                    endText = primaryBylineEnd,
                    textStyle = AppTheme.typography.paragraph2,
                    textColor = AppTheme.colors.title
                )
            }
        },
        contentStart = {
            Image(
                imageResource = startImageResource,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 16.dp)
                    .size(24.dp),
                coilImageBuilderScope = null
            )
        },
        contentBottom = {
            TableRowText(
                startText = secondaryBylineStart,
                endText = secondaryBylineEnd,
                textStyle = AppTheme.typography.paragraph1,
                textColor = AppTheme.colors.body,
                modifier = Modifier.padding(start = 40.dp)
            )
        },
        onContentClicked = onClick
    )
}

@Preview
@Composable
private fun BalanceTableRowLargePreview() {
    AppTheme {
        AppSurface {
            BalanceTableRowLarge(
                title = "Bitcoin",
                historicalRates = List(10) {
                    object : SparkLineHistoricalRate {
                        override val timestamp: Long = it.toLong()
                        override val rate: Double = Math.random() * 1000
                    }
                },
                primaryBylineStart = buildAnnotatedString { append("\$15,879.90") },
                primaryBylineEnd = buildAnnotatedString { append("\$44,403.13") },
                secondaryBylineStart = buildAnnotatedString { append("0.3576301941 BTC") },
                secondaryBylineEnd = buildAnnotatedString { append("↓ 12.32%") },
                startImageResource = ImageResource.Remote(
                    url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                    contentDescription = null,
                ),
                onClick = null
            )
        }
    }
}