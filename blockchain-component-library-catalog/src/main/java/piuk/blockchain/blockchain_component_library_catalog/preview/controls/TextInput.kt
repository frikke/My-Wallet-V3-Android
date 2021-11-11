package piuk.blockchain.blockchain_component_library_catalog.preview.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "TextInput", group = "Controls")
@Composable
fun TextInput_Preview() {
    var value by remember { mutableStateOf("")}
    AppTheme {
        AppSurface {
            TextInput(
                value = value,
                onValueChange = {
                    value = it
                },
                assistiveText = "This is assistive text",
                isError = value == "Error",
                errorMessage = "This is an error message",
                label = "Label",
                placeholder = "Type Error to show error state"
            )
        }
    }
}

@Preview(name = "TextInput Error", group = "Controls")
@Composable
fun TextInputError_Preview() {
    var value by remember { mutableStateOf("")}
    AppTheme {
        AppSurface {
            TextInput(
                value = value,
                onValueChange = {
                    value = it
                },
                isError = true,
                label = "Label",
                errorMessage = "This is an error message"
            )
        }
    }
}

@Preview(name = "TextInput Disabled", group = "Controls")
@Composable
fun TextInputDisabled_Preview() {
    var value by remember { mutableStateOf("")}
    AppTheme {
        AppSurface {
            TextInput(
                value = value,
                onValueChange = {
                    value = it
                },
                enabled = false,
                placeholder = "I'm disabled"
            )
        }
    }
}