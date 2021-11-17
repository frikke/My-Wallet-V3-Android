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
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.sectionheader.ExchangeSectionHeader
import com.blockchain.componentlib.sectionheader.ExchangeSectionHeaderType
import com.blockchain.componentlib.sectionheader.WalletBalanceSectionHeader
import com.blockchain.componentlib.sectionheader.WalletSectionHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Exchange Section Header")
@Composable
fun ExchangeSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            ExchangeSectionHeader(
                headerType = ExchangeSectionHeaderType.Default(
                    title = "Destination Address",
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Icon", group = "Exchange Section Header")
@Composable
fun ExchangeSectionHeaderIconPreview() {
    AppTheme {
        AppSurface {
            ExchangeSectionHeader(
                headerType = ExchangeSectionHeaderType.Icon(
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

@Preview(name = "Filter", group = "Exchange Section Header")
@Composable
fun ExchangeSectionHeaderFilterPreview() {
    var selected by remember { mutableStateOf(0) }
    AppTheme {
        AppSurface {
            ExchangeSectionHeader(
                headerType = ExchangeSectionHeaderType.Filter(
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

@Preview(name = "Wallet Balance", group = "Wallet Section Header")
@Composable
fun WalletBalanceSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            WalletBalanceSectionHeader(
                primaryText = "\$12,293.21",
                secondaryText = "0.1393819 BTC",
                buttonText = "Buy BTC",
                onButtonClick = {},
            )
        }
    }
}

@Preview(name = "Default", group = "Wallet Section Header")
@Composable
fun WalletSectionHeaderPreview() {
    AppTheme {
        AppSurface {
            WalletSectionHeader("Title", Modifier.fillMaxWidth())
        }
    }
}