package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.controls.TextInputView
import piuk.blockchain.blockchain_component_library_catalog.R

class TextInputActivity : AppCompatActivity() {

    private var defaultValue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_input)

        findViewById<TextInputView>(R.id.default_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
                isError = defaultValue == "Error"
            }
            assistiveText = "This is assistive text"
            errorText = "This is an error message"
            labelText = "Label"
            placeholderText = "Type Error to show error state"
            trailingIcon = R.drawable.ic_alert
        }

        findViewById<TextInputView>(R.id.error_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
            }
            isError = true
            errorText = "This is an error message"
            labelText = "Label"
            placeholderText = "Placeholder"
            trailingIcon = R.drawable.ic_alert
        }

        findViewById<TextInputView>(R.id.disabled_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
            }
            isInputEnabled = false
            labelText = "Label"
            assistiveText = "This is disabled assistive text"
            trailingIcon = R.drawable.ic_alert
        }
    }
}