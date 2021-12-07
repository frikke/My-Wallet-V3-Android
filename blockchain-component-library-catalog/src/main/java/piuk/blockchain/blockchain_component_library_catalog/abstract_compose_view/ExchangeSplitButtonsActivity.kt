package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.ExchangeSplitButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class ExchangeSplitButtonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_split_buttons)

        findViewById<ExchangeSplitButtonView>(R.id.enabled).apply {
            onPrimaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            primaryButtonText = "Enabled"
            primaryButtonState = ButtonState.Enabled
            onSecondaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            secondaryButtonText = "Enabled"
            secondaryButtonState = ButtonState.Enabled
        }

        findViewById<ExchangeSplitButtonView>(R.id.disabled).apply {
            onPrimaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            primaryButtonText = "Disabled"
            primaryButtonState = ButtonState.Disabled
            onSecondaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            secondaryButtonText = "Disabled"
            secondaryButtonState = ButtonState.Disabled
        }

        findViewById<ExchangeSplitButtonView>(R.id.loading).apply {
            onPrimaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            primaryButtonText = "Loading"
            primaryButtonState = ButtonState.Loading
            onSecondaryButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            secondaryButtonText = "Loading"
            secondaryButtonState = ButtonState.Loading
        }
    }
}