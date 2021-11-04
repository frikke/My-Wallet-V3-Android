package piuk.blockchain.blockchain_component_library_catalog.preview.tablerow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.tablerow.DefaultStackedIconTableRow
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.ToggleTableRow
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Table Row")
@Composable
fun DefaultTableRowPreview() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
                primaryText = "Navigate over here",
                secondaryText = "Text for more info",
                onClick = {},
            )
        }
    }
}

@Preview(name = "Tag", group = "Table Row")
@Composable
fun TagTableRowPreview() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
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

@Preview(name = "Stacked Icon", group = "Table Row")
@Composable
fun StackedIconTableRowPreview() {
    AppTheme {
        AppSurface {
            DefaultStackedIconTableRow(
                primaryText = "Primary text",
                secondaryText = "Secondary text",
                iconTopUrl = "https://www.blockchain.com/static/img/prices/prices-btc.svg",
                iconBottomUrl = "https://www.blockchain.com/static/img/prices/prices-eth.svg",
                onClick = {},
            )
        }
    }
}

@Preview(name = "Large", group = "Table Row")
@Composable
fun LargeTableRowPreview() {
    AppTheme {
        AppSurface {
            DefaultTableRow(
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

@Preview(name = "Toggle", group = "Table Row")
@Composable
fun ToggleTableRowPreview() {
    AppTheme {
        AppSurface {
            var isChecked by remember { mutableStateOf(false) }
            ToggleTableRow(
                onCheckedChange = { isChecked = it },
                title = "Enable this ?",
                body = "Some additional info",
                isChecked = isChecked
            )
        }
    }
}