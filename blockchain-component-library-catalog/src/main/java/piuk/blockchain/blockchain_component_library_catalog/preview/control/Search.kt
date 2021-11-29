package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Search
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default Search", group = "Search")
@Composable
fun SearchPreview() {

    AppTheme {
        AppSurface {
            Search(
                label = "Search Coins"
            )
        }
    }
}