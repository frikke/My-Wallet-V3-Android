package piuk.blockchain.android.ui.interest.presentation.composables

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.controls.TextInput
import piuk.blockchain.android.R

@Composable
fun SearchField(onSearchTermChanged: (String) -> Unit) {
    var text: String by remember { mutableStateOf("") }

    TextInput(
        value = text,
        placeholder = "Search", // todo when we get designs
        onValueChange = {
            text = it
            onSearchTermChanged(it)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        trailingIcon = ImageResource.Local(id = R.drawable.ic_search),
    )
}

@Preview
@Composable
private fun PreviewSearchField() {
    SearchField {}
}