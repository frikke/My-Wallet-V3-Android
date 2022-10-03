package com.blockchain.componentlib.control

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.controls.TextInput
import com.blockchain.componentlib.controls.TextInputState

@Composable
fun DropdownMenuSearch(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    initialSuggestions: List<String>,
    label: String? = null,
    state: TextInputState = TextInputState.Default(""),
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldValueState by remember { mutableStateOf(value) }

    val interactionSource = remember { MutableInteractionSource() }
    if (interactionSource.collectIsPressedAsState().value) expanded = true

    var trailingIcon =
        if (textFieldValueState.text.isEmpty()) ImageResource.None
        else ImageResource.Local(R.drawable.ic_close)

    Column {
        TextInput(
            value = textFieldValueState.text,
            onValueChange = {
                expanded = true
                textFieldValueState = TextFieldValue(it)
                onValueChange(textFieldValueState)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { expanded = it.hasFocus },
            label = label,
            trailingIcon = trailingIcon,
            onTrailingIconClicked = {
                textFieldValueState = TextFieldValue(text = "")
            },
            interactionSource = interactionSource,
            state = state
        )

        if (initialSuggestions.isNotEmpty()) {
            Box {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier.requiredSizeIn(maxHeight = 200.dp)
                ) {
                    initialSuggestions.filter { it.contains(textFieldValueState.text, ignoreCase = true) }
                        .forEach { label ->
                            DropdownMenuItem(onClick = {
                                textFieldValueState = TextFieldValue(text = label, selection = TextRange(label.length))
                                onValueChange(TextFieldValue(text = label))
                                expanded = false
                                trailingIcon = ImageResource.Local(R.drawable.ic_close)
                            }) {
                                Text(text = label)
                            }
                        }
                }
            }
        }
    }
}
