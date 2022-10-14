package piuk.blockchain.android.ui.settings.security.pin.requests

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.presentation.BackupPhrasePinService
import com.blockchain.ui.password.SecondPasswordHandler
import piuk.blockchain.android.ui.settings.security.pin.PinActivity
import piuk.blockchain.android.ui.settings.security.pin.PinActivity.Companion.OriginScreenToPin

class BackupPhrasePinRequest(
    private val secondPasswordHandler: SecondPasswordHandler
) : BackupPhrasePinService {

    private var onPinVerificationResult: ActivityResultLauncher<Intent>? = null
    private var activity: BlockchainActivity? = null
    private lateinit var callback: (successful: Boolean, secondPassword: String?) -> Unit

    override fun init(activity: BlockchainActivity) {
        require(activity.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
            "should be called in onCreate"
        }

        this.activity = activity

        onPinVerificationResult = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                pinCodeVerified()
            } else {
                callback(false, null)
            }
        }
    }

    private fun pinCodeVerified() {
        require(activity != null) {
            "activity is null"
        }

        require(activity!!.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "lifecycle state is ${activity!!.lifecycle.currentState}, should be at least STARTED"
        }

        secondPasswordHandler.validate(
            activity!!,
            object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    callback(true, null)
                }

                override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                    callback(true, validatedSecondPassword)
                }
            }
        )
    }

    override fun verifyPin(callback: (successful: Boolean, secondPassword: String?) -> Unit) {
        require(activity != null && onPinVerificationResult != null) {
            "init must be called first in onCreate"
        }

        require(activity!!.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "lifecycle state is ${activity!!.lifecycle.currentState}, should be at least STARTED"
        }

        this.callback = callback

        onPinVerificationResult!!.launch(
            PinActivity.newIntent(
                context = activity!!,
                startForResult = true,
                originScreen = OriginScreenToPin.BACKUP_PHRASE,
                addFlagsToClear = false
            )
        )
    }
}
