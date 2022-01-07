package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.PagerIndicatorDots
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import kotlinx.coroutines.delay

@Preview(name = "Default", group = "Pager")
@Composable
fun PagerIndicatorDotsPreview() {
    var index by remember { mutableStateOf(0) }
    val count = 5
    AppTheme {
        AppSurface {
            PagerIndicatorDots(selectedIndex = index, count = count)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            index = (index + 1) % count
        }
    }
}