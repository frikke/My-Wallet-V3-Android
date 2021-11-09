package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Minimal button")
@Composable
fun MinimalButton_Basic() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
            )
        }
    }
}

@Preview(name = "Loading", group = "Minimal button")
@Composable
fun MinimalButton_Loading() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Loading,
            )
        }
    }
}

@Preview(name = "Disabled", group = "Minimal button")
@Composable
fun MinimalButton_Disabled() {
    AppTheme {
        AppSurface {
            MinimalButton(
                onClick = { },
                text = "Button",
                state = ButtonState.Disabled,
            )
        }
    }
}