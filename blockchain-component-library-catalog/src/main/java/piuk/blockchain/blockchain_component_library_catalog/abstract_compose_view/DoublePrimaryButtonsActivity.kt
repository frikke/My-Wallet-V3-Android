package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DoublePrimaryButtonsView
import piuk.blockchain.blockchain_component_library_catalog.R

class DoublePrimaryButtonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_double_primary_buttons)
        
        findViewById<DoublePrimaryButtonsView>(R.id.enabled).apply {
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

        findViewById<DoublePrimaryButtonsView>(R.id.disabled).apply {
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

        findViewById<DoublePrimaryButtonsView>(R.id.loading).apply {
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