package piuk.blockchain.blockchain_component_library_catalog.preview.control

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.control.PrimarySwitch
import com.blockchain.componentlib.control.SuccessSwitch
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Not checked", group = "Primary Switch")
@Composable
fun PrimarySwitchPreview_NotChecked() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = false,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Primary Switch")
@Composable
fun PrimarySwitchPreview_IsChecked() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = true,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Primary Switch")
@Composable
fun PrimarySwitchPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = false,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Primary Switch")
@Composable
fun PrimarySwitchPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            PrimarySwitch(
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Not checked", group = "Success Switch")
@Composable
fun SuccessSwitchPreview_NotChecked() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = false,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Checked", group = "Success Switch")
@Composable
fun SuccessSwitchPreview_IsChecked() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = true,
                onCheckChanged = {},
            )
        }
    }
}

@Preview(name = "Not checked not enabled", group = "Success Switch")
@Composable
fun SuccessSwitchPreview_NotChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = false,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}

@Preview(name = "Checked not enabled", group = "Success Switch")
@Composable
fun SuccessSwitchPreview_IsChecked_NotEnabled() {
    AppTheme {
        AppSurface {
            SuccessSwitch(
                isChecked = true,
                onCheckChanged = {},
                enabled = false,
            )
        }
    }
}
