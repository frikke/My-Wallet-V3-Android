package piuk.blockchain.blockchain_component_library_catalog.preview.sectionheader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.sectionheader.LargeSectionHeader
import com.blockchain.componentlib.sectionheader.LargeSectionHeaderType
import com.blockchain.componentlib.sectionheader.BalanceSectionHeader
import com.blockchain.componentlib.sectionheader.SmallSectionHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Large Section Header")
@Composable
fun LargeSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Default(
                    title = "Destination Address",
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Icon", group = "Large Section Header")
@Composable
fun LargeSectionHeaderIconPreview() {
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Icon(
                    title = "Destination Address",
                    icon = ImageResource.Local(
                        id = R.drawable.ic_qr_code,
                        contentDescription = null,
                    ),
                    onIconClicked = {}
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Filter", group = "Large Section Header")
@Composable
fun LargeSectionHeaderFilterPreview() {
    var selected by remember { mutableStateOf(0) }
    AppTheme {
        AppSurface {
            LargeSectionHeader(
                headerType = LargeSectionHeaderType.Filter(
                    title = "Destination Address",
                    options = listOf("USD", "GBP", "EUR"),
                    onOptionSelected = { selected = it },
                    optionIndexSelected = selected
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Balance", group = "Balance Section Header")
@Composable
fun BalanceSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            BalanceSectionHeader(
                primaryText = "\$12,293.21",
                secondaryText = "0.1393819 BTC"
            )
        }
    }
}

@Preview(name = "Small", group = "Small Section Header")
@Composable
fun SmallSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            SmallSectionHeader("Title", Modifier.fillMaxWidth())
        }
    }
}