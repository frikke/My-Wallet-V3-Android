package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButtonView
import com.blockchain.componentlib.button.SecondaryButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class SecondaryButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondary_button)

        findViewById<SecondaryButtonView>(R.id.enabled).apply {
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

        findViewById<SecondaryButtonView>(R.id.icon).apply {
            onClick = {
                Toast
                    .makeText(
                        this@SecondaryButtonActivity,
                        "This button has an icon",
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
            text = "Icon"
            buttonState = ButtonState.Enabled
            icon = ImageResource.Local(
                id = R.drawable.ic_qr_code,
                contentDescription = null,
            )
        }

        findViewById<SecondaryButtonView>(R.id.disabled).apply {
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

        findViewById<SecondaryButtonView>(R.id.loading).apply {
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