package piuk.blockchain.blockchain_component_library_catalog.preview.tablerow

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.tablerow.ActionStackedIconTableRow
import com.blockchain.componentlib.tablerow.ActionTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Action Table Row")
@Composable
fun ActionDefaultTableRowPreview() {
    AppTheme {
        AppSurface {
            ActionTableRow(
                startIconUrl = "https://exchange.blockchain.com/static/img/mercury/landing/england-flag@2x.png",
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
            )
        }
    }
}

@Preview(name = "Tag", group = "Action Table Row")
@Composable
fun ActionTagTableRowPreview() {
    AppTheme {
        AppSurface {
            ActionTableRow(
                startIconUrl = "https://exchange.blockchain.com/static/img/mercury/landing/england-flag@2x.png",
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
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
            )
        }
    }
}

@Preview(name = "Stacked Icon", group = "Action Table Row")
@Composable
fun ActionStackedIconTableRowPreview() {
    AppTheme {
        AppSurface {
            ActionStackedIconTableRow(
                primaryText = "Primary text",
                secondaryText = "Secondary text",
                iconTopUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                iconBottomUrl = "https://www.blockchain.com/static/img/prices/prices-eth.svg",
                onClick = {},
            )
        }
    }
}

@Preview(name = "Large", group = "Action Table Row")
@Composable
fun ActionLargeTableRowPreview() {
    AppTheme {
        AppSurface {
            ActionTableRow(
                startIconUrl = "https://exchange.blockchain.com/static/img/mercury/landing/england-flag@2x.png",
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                paragraphText = "This is a long paragraph which wraps, This is a long paragraph which wraps, This is a long paragraph which wraps, This is a long paragraph which wraps, This is a long paragraph which wraps,",
                onClick = {},
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
            )
        }
    }
}