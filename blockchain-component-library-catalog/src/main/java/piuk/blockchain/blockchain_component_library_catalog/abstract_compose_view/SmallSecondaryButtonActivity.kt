package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButtonView
import com.blockchain.componentlib.button.SmallSecondaryButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class SmallSecondaryButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_small_secondary_button)

        findViewById<SmallSecondaryButtonView>(R.id.enabled).apply {
            onClick = {
                Toast
                    .makeText(
                        context,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Enabled"
            buttonState = ButtonState.Enabled
        }

        findViewById<SmallSecondaryButtonView>(R.id.disabled).apply {
            onClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Disabled"
            buttonState = ButtonState.Disabled
        }

        findViewById<SmallSecondaryButtonView>(R.id.loading).apply {
            onClick = {
                Toast
                    .makeText(
                        context,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Loading"
            buttonState = ButtonState.Loading
        }
    }
}