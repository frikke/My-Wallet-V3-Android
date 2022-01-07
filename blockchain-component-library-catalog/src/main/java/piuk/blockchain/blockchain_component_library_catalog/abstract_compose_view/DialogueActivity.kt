package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCardView
import piuk.blockchain.blockchain_component_library_catalog.R

class DialogueActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialogue)

        findViewById<DialogueCardView>(R.id.dialogue_card).apply {
            title = "Test title"
            body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt" +
                " ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            firstButton = DialogueButton("First button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }

            secondButton = DialogueButton("Second button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<DialogueCardView>(R.id.small_dialogue).apply {
            body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt" +
                " ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            firstButton = DialogueButton("First button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }

            secondButton = DialogueButton("Second button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        findViewById<DialogueCardView>(R.id.centered_dialogue).apply {
            title = "Test title"
            body = "Body 2: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt" +
                " ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
            icon = R.drawable.ic_bottom_nav_home
            firstButton = DialogueButton("First button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }

            secondButton = DialogueButton("Second button") {
                Toast.makeText(this@DialogueActivity, "Clicked", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}