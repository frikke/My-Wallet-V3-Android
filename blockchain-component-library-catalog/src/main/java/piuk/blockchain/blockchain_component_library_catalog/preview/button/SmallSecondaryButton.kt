package piuk.blockchain.blockchain_component_library_catalog.preview.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SmallSecondaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default", group = "Small secondary button")
@Composable
fun SmallSecondaryButtonPreview() {
    AppTheme {
        AppSurface {
            SmallSecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Enabled
            )
        }
    }
}

@Preview(name = "Disabled", group = "Small secondary button")
@Composable
fun SmallSecondaryButtonDisabledPreview() {
    AppTheme {
        AppSurface {
            SmallSecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Disabled
            )
        }
    }
}

@Preview(name = "Loading", group = "Small secondary button")
@Composable
fun SmallSecondaryButtonLoadingPreview() {
    AppTheme {
        AppSurface {
            SmallSecondaryButton(
                text = "Click me",
                onClick = { },
                state = ButtonState.Loading
            )
        }
    }
}
