package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Primary button")
@Composable
fun PrimaryButtonPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Primary button")
@Composable
fun PrimaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Primary button")
@Composable
fun PrimaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            PrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
