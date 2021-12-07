package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.controls.TextInputView
import com.blockchain.componentlib.image.ImageResource
import piuk.blockchain.blockchain_component_library_catalog.R

class TextInputActivity : AppCompatActivity() {

    private var defaultValue = ""

    val errorState = TextInputState.Error("This is an error message")
    val defaultState = TextInputState.Default("This is assistive text")
    val successState = TextInputState.Success("This is a success message")
    val disabledState = TextInputState.Disabled("This is a disabled assistive text")

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_input)

        findViewById<TextInputView>(R.id.default_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
                state = if(defaultValue == "Error") {
                    errorState
                } else {
                    defaultState
                }
            }
            state = defaultState
            labelText = "Label"
            placeholderText = "Type Error to show error state"
            trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
        }

        findViewById<TextInputView>(R.id.error_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
            }
            state = errorState
            labelText = "Label"
            placeholderText = "Placeholder"
            trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
        }

            findViewById<TextInputView>(R.id.success_text_input).apply {
                value = defaultValue
                onValueChange = {
                    defaultValue = it
                    value = defaultValue
                }
                labelText = "Label"
                state = successState
                trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
            }

        findViewById<TextInputView>(R.id.disabled_text_input).apply {
            value = defaultValue
            onValueChange = {
                defaultValue = it
                value = defaultValue
            }
            labelText = "Label"
            state = disabledState
            trailingIconResource = ImageResource.Local(R.drawable.ic_alert, null)
        }
    }
}