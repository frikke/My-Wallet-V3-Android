package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.ExchangeSplitButtonView
import com.blockchain.componentlib.button.SplitButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class SplitButtonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_buttons)

        findViewById<SplitButtonView>(R.id.enabled).apply {
            onStartButtonClick = {
                Toast
                    .makeText(
                        context,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            startButtonText = "Enabled"
            startButtonState = ButtonState.Enabled
            onEndButtonClick = {
                Toast
                    .makeText(
                        context,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            endButtonText = "Enabled"
            endButtonState = ButtonState.Enabled
        }

        findViewById<SplitButtonView>(R.id.disabled).apply {
            onStartButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            startButtonText = "Disabled"
            startButtonState = ButtonState.Disabled
            onEndButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            endButtonText = "Disabled"
            endButtonState = ButtonState.Disabled
        }

        findViewById<SplitButtonView>(R.id.loading).apply {
            onStartButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            startButtonText = "Loading"
            startButtonState = ButtonState.Loading
            onEndButtonClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            endButtonText = "Loading"
            endButtonState = ButtonState.Loading
        }
    }
}