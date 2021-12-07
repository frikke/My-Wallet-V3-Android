package piuk.blockchain.blockchain_component_library_catalog.preview.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

private val errorState = TextInputState.Error("This is an error message")
private val defaultState = TextInputState.Default("This is assistive text")
private val successState = TextInputState.Success("This is a success message")
private val disabledState = TextInputState.Disabled("This is a disabled assistive text")

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
                state = if(value == "Error") {
                    errorState
                } else {
                    defaultState
                },
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
                state = errorState,
                label = "Label"
            )
        }
    }
}

@Preview(name = "TextInput Success", group = "Controls")
@Composable
fun TextInputSuccess_Preview() {
    var value by remember { mutableStateOf("")}
    AppTheme {
        AppSurface {
            TextInput(
                value = value,
                onValueChange = {
                    value = it
                },
                state = successState,
                placeholder = "I'm disabled"
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
                state = disabledState,
                placeholder = "I'm disabled"
            )
        }
    }
}