package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DoubleMinimalButtonsView
import com.blockchain.componentlib.basic.ImageResource
import piuk.blockchain.blockchain_component_library_catalog.R

class DoubleMinimalButtonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_double_minimal_buttons)

        findViewById<DoubleMinimalButtonsView>(R.id.enabled).apply {
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

        findViewById<DoubleMinimalButtonsView>(R.id.disabled).apply {
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

        findViewById<DoubleMinimalButtonsView>(R.id.loading).apply {
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

        findViewById<DoubleMinimalButtonsView>(R.id.icon).apply {
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
            startButtonIcon = ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_qr_code,
                contentDescription = null,
            )
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
            endButtonIcon = ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_qr_code,
                contentDescription = null,
            )
        }
    }
}