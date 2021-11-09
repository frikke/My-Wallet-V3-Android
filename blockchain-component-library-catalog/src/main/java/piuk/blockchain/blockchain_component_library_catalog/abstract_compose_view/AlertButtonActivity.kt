package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.button.AlertButtonView
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class AlertButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_button)

        findViewById<AlertButtonView>(R.id.enabled).apply {
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

        findViewById<AlertButtonView>(R.id.loading).apply {
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