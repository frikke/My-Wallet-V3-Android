package piuk.blockchain.blockchain_component_library_catalog.preview.tablerow

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tablerow.BalanceStackedIconTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRowLarge
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Balance Row")
@Composable
fun BalanceTableRowPreview() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString {
                    append("Bitcoin")
                },
                titleEnd = buildAnnotatedString {
                    append("\$44,403.13")
                },
                bodyStart = buildAnnotatedString {
                    append("BTC")
                },
                bodyEnd = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                        append("↓ 12.32%")
                    }
                },
                startImageResource = ImageResource.Remote(
                    url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                    contentDescription = null,
                ),
                tags = listOf(),
                onClick = {},
            )
        }
    }
}

@Preview(name = "Tag", group = "Balance Row")
@Composable
fun BalanceTagTableRowPreview() {
    AppTheme {
        AppSurface {
            BalanceTableRow(
                titleStart = buildAnnotatedString {
                    append("Bitcoin")
                },
                titleEnd = buildAnnotatedString {
                    append("\$44,403.13")
                },
                bodyStart = buildAnnotatedString {
                    append("BTC")
                },
                bodyEnd = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                        append("↓ 12.32%")
                    }
                },
                startImageResource = ImageResource.Remote(
                    url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                    contentDescription = null,
                ),
                tags = listOf(
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                    TagViewState(
                        value = "Completed",
                        type = TagType.Success
                    ),
                    TagViewState(
                        value = "Warning",
                        type = TagType.Warning
                    ),
                ),
                onClick = {},
            )
        }
    }
}

@Preview(name = "Stacked Icon", group = "Balance Row")
@Composable
fun BalanceStackedIconTableRowPreview() {
    AppTheme {
        AppSurface {
            BalanceStackedIconTableRow(
                titleStart = buildAnnotatedString {
                    append("Bitcoin")
                },
                titleEnd = buildAnnotatedString {
                    append("\$44,403.13")
                },
                bodyStart = buildAnnotatedString {
                    append("BTC")
                },
                bodyEnd = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = AppTheme.colors.error)) {
                        append("↓ 12.32%")
                    }
                },
                topImageResource = ImageResource.Remote(
                    url = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                    contentDescription = null,
                ),
                bottomImageResource = ImageResource.Remote(
                    url = "https://www.blockchain.com/static/img/prices/prices-eth.svg",
                    contentDescription = null,
                ),
                onClick = {},
            )
        }
    }
}

@Preview(name = "Large", group = "Balance Row")
@Composable
fun BalanceTableRowLargePreview() {
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
                onClick = { }
            )
        }
    }
}