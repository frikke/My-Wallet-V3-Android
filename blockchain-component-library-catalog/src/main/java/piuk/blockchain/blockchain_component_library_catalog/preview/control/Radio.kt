package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Radio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Not checked", group = "Radio")
@Composable
fun RadioPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Unselected,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Error", group = "Radio")
@Composable
fun RadioPreview_Error() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Error,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Radio")
@Composable
fun RadioPreview_IsChecked() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Selected,
                onSelectedChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Radio")
@Composable
fun RadioPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Unselected,
                onSelectedChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Radio")
@Composable
fun RadioPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Radio(
                state = RadioButtonState.Selected,
                onSelectedChanged = {},
                enabled = false,
            )
        }
    }
}
