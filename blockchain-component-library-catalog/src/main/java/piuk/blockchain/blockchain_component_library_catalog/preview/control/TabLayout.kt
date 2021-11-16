package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.TabLayoutLarge
import com.blockchain.componentlib.control.TabLayoutLive
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Live", group = "Tab layout")
@Composable
fun TabLayoutLivePreview() {
    var selectedItem by remember { mutableStateOf(1) }
    AppTheme {
        AppSurface {
            TabLayoutLive(
                items = listOf("Live", "1D", "1W", "1M", "1Y", "All"),
                onItemSelected = { index -> selectedItem = index },
                modifier = Modifier.fillMaxWidth(),
                selectedItemIndex = selectedItem
            )
        }
    }
}

@Preview(name = "Large", group = "Tab layout")
@Composable
fun TabLayoutLargePreview() {
    var selectedItem by remember { mutableStateOf(0) }
    AppTheme {
        AppSurface {
            TabLayoutLarge(
                items = listOf("First", "Second", "Third"),
                onItemSelected = { index -> selectedItem = index },
                modifier = Modifier.fillMaxWidth(),
                selectedItemIndex = selectedItem
            )
        }
    }
}