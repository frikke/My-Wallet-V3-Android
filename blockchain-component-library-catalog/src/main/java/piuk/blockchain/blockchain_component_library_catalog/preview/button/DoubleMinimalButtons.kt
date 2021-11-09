package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.DoubleMinimalButtons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Double minimal buttons")
@Composable
fun DoubleMinimalButtonsPreview() {
    AppTheme {
        AppSurface {
            DoubleMinimalButtons(
                startButtonText = "Primary",
                onStartButtonClick = { },
                endButtonText = "Secondary",
                onEndButtonClick = { },
            )
        }
    }
}