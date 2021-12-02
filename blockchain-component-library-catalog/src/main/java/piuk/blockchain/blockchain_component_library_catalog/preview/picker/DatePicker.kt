package piuk.blockchain.blockchain_component_library_catalog.preview.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.pickers.DateCalendar
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
@Preview(name = "DateCalendar", group = "Pickers")
fun DateCalendar_Preview() {
    AppTheme {
        AppSurface {
            DateCalendar()
        }
    }
}