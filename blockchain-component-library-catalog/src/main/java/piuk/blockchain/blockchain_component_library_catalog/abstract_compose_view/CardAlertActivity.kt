package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlertView
import piuk.blockchain.blockchain_component_library_catalog.R

class CardAlertActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_alert)

        findViewById<CardAlertView>(R.id.default_card).apply {
            title = "Default title"
            subtitle = "Default subtitle"
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.success_card).apply {
            title = "Success title"
            subtitle = "success subtitle"
            alertType = AlertType.Success
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.warning_card).apply {
            title = "Warning title"
            subtitle = "Warning subtitle"
            alertType = AlertType.Warning
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.error_card).apply {
            title = "Error title"
            subtitle = "Error subtitle"
            alertType = AlertType.Error
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.default_card_bordered).apply {
            title = "Default title"
            subtitle = "Default subtitle"
            isBordered = true
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.success_card_bordered).apply {
            title = "Success title"
            subtitle = "success subtitle"
            isBordered = true
            alertType = AlertType.Success
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.warning_card_bordered).apply {
            title = "Warning title"
            subtitle = "Warning subtitle"
            isBordered = true
            alertType = AlertType.Warning
            onClose = {
                this.isVisible = false
            }
        }

        findViewById<CardAlertView>(R.id.error_card_bordered).apply {
            title = "Error title"
            subtitle = "Error subtitle"
            isBordered = true
            alertType = AlertType.Error
            onClose = {
                this.isVisible = false
            }
        }
    }
}