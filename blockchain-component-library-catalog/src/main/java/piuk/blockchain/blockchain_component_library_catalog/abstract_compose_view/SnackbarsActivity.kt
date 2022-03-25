package piuk.blockchain.blockchain_component_library_catalog.abstract_compose_view

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blockchain.componentlib.alert.SnackbarAlertView
import com.blockchain.componentlib.alert.SnackbarType
import piuk.blockchain.blockchain_component_library_catalog.R

class SnackbarsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snackbars)

        findViewById<SnackbarAlertView>(R.id.info).apply {
            message = "Info message"
        }
        findViewById<SnackbarAlertView>(R.id.info_with_action).apply {
            message = "Info message"
            actionLabel = "Action label"
            onClick = { Toast.makeText(this@SnackbarsActivity, "action clicked", Toast.LENGTH_SHORT).show() }
        }
        findViewById<SnackbarAlertView>(R.id.success).apply {
            message = "Success message"
            type = SnackbarType.Success
        }
        findViewById<SnackbarAlertView>(R.id.warning).apply {
            message = "Warning message"
            type = SnackbarType.Warning
        }
        findViewById<SnackbarAlertView>(R.id.error).apply {
            message = "Error message"
            type = SnackbarType.Error
        }
        findViewById<SnackbarAlertView>(R.id.success_with_action).apply {
            message = "Success message"
            actionLabel = "Action label"
            type = SnackbarType.Success
            onClick = { Toast.makeText(this@SnackbarsActivity, "action clicked", Toast.LENGTH_SHORT).show() }
        }
        findViewById<SnackbarAlertView>(R.id.error_with_action).apply {
            message = "Error message"
            actionLabel = "Action label"
            type = SnackbarType.Error
            onClick = { Toast.makeText(this@SnackbarsActivity, "action clicked", Toast.LENGTH_SHORT).show() }
        }
        findViewById<SnackbarAlertView>(R.id.warning_with_action).apply {
            message = "Warning message"
            actionLabel = "Action label"
            type = SnackbarType.Warning
            onClick = { Toast.makeText(this@SnackbarsActivity, "action clicked", Toast.LENGTH_SHORT).show() }
        }
    }
}