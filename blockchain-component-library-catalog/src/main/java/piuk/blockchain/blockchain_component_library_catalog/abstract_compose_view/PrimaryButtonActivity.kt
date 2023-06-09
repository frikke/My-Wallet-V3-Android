package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButtonView
import com.blockchain.componentlib.basic.ImageResource
import piuk.blockchain.blockchain_component_library_catalog.R

class PrimaryButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primary_button)

        findViewById<PrimaryButtonView>(R.id.enabled).apply {
            onClick = {
                Toast
                    .makeText(
                        this@PrimaryButtonActivity,
                        "Clicked",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Enabled"
            buttonState = ButtonState.Enabled
        }

        findViewById<PrimaryButtonView>(R.id.disabled).apply {
            onClick = {
                Toast
                    .makeText(
                        this@PrimaryButtonActivity,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Disabled"
            buttonState = ButtonState.Disabled
        }

        findViewById<PrimaryButtonView>(R.id.loading).apply {
            onClick = {
                Toast
                    .makeText(
                        this@PrimaryButtonActivity,
                        "This toast shouldn't show up",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Loading"
            buttonState = ButtonState.Loading
        }

        findViewById<PrimaryButtonView>(R.id.icon).apply {
            onClick = {
                Toast
                    .makeText(
                        this@PrimaryButtonActivity,
                        "This button has an icon",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Icon"
            buttonState = ButtonState.Enabled
            icon = ImageResource.Local(
                id = com.blockchain.componentlib.R.drawable.ic_qr_code,
                contentDescription = null,
            )
        }
    }
}