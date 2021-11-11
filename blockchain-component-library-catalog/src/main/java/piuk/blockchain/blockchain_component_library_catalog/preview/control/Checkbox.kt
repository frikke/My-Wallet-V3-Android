package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.Checkbox
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Not checked", group = "Checkbox")
@Composable
fun CheckboxPreview_NotChecked() {
    AppTheme {
        AppSurface {
            Checkbox(
                isChecked = false,
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
                isChecked = true,
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
                isChecked = false,
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
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}