package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.control.CheckboxState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Not checked", group = "Checkbox")
@Composable
fun CheckboxPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Unchecked,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Error", group = "Checkbox")
@Composable
fun CheckboxPreview_Error() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Error,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Checkbox")
@Composable
fun CheckboxPreview_IsChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Checked,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Checkbox")
@Composable
fun CheckboxPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Unchecked,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Checkbox")
@Composable
fun CheckboxPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            Checkbox(
                state = CheckboxState.Checked,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
