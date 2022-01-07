package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Small minimal button")
@Composable
fun SmallMinimalButton_Basic() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button"
            )
        }
    }
}

@Preview(name = "Loading", group = "Small minimal button")
@Composable
fun SmallMinimalButton_Loading() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small minimal button")
@Composable
fun SmallMinimalButton_Disabled() {
    AppTheme {
        AppSurface {
            SmallMinimalButton(
                onClick = { },
                text = "Small Minimal button",
                state = ButtonState.Disabled,
            )
        }
    }
}
