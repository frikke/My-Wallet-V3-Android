package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallPrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Small primary button")
@Composable
fun SmallPrimaryButtonPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small primary button")
@Composable
fun SmallPrimaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Small primary button")
@Composable
fun SmallPrimaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            SmallPrimaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
