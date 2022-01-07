package piuk.blockchain.blockchain_component_library_catalog.preview.tablerow

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.tablerow.ActionStackedIconTableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Stacked Icon", group = "Action Table Row")
@Composable
fun ActionStackedIconTableRowPreview() {
    AppTheme {
        AppSurface {
            ActionStackedIconTableRow(
                primaryText = "Primary text",
                secondaryText = "Secondary text",
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
