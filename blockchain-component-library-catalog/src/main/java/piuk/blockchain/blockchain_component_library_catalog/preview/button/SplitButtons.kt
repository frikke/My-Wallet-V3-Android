package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.SplitButtons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Split buttons")
@Composable
fun SplitButtonPreview() {
    AppTheme {
        AppSurface {
            SplitButtons(
                primaryButtonText = "Primary",
                primaryButtonOnClick = { },
                secondaryButtonText = "Secondary",
                secondaryButtonOnClick = { }
            )
        }
    }
}