package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.DoublePrimaryButtons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Double primary buttons", device = Devices.PIXEL)
@Composable
fun DoublePrimaryButtons() {
    AppTheme {
        AppSurface {
            DoublePrimaryButtons(
                startButtonText = "Primary",
                onStartButtonClick = { },
                endButtonText = "Secondary",
                onEndButtonClick = { },
            )
        }
    }
}