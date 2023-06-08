package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.DestructiveOutlinedButtonView
import com.blockchain.componentlib.button.PrimaryOutlinedButtonView
import piuk.blockchain.blockchain_component_library_catalog.R

class MinimalButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minimal_button)

        findViewById<PrimaryOutlinedButtonView>(R.id.enabled).apply {
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

        findViewById<PrimaryOutlinedButtonView>(R.id.icon).apply {
            onClick = {
                Toast
                    .makeText(
                        this@MinimalButtonActivity,
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

        findViewById<PrimaryOutlinedButtonView>(R.id.disabled).apply {
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

        findViewById<PrimaryOutlinedButtonView>(R.id.loading).apply {
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

        findViewById<DestructiveOutlinedButtonView>(R.id.destructive_enabled).apply {
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

        findViewById<DestructiveOutlinedButtonView>(R.id.destructive_disabled).apply {
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

        findViewById<DestructiveOutlinedButtonView>(R.id.destructive_loading).apply {
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