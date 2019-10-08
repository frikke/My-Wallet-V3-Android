package piuk.blockchain.android.ui.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.StringRes
import android.support.annotation.UiThread
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import android.view.WindowManager
import com.blockchain.koin.injectActivity
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.ui.dialog.MaterialProgressDialog
import com.blockchain.ui.password.SecondPasswordHandler
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import piuk.blockchain.androidcoreui.R
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */

abstract class BlockchainActivity : ToolBarActivity() {

    private val logoutTimer: LogoutTimer by inject()
    private val securityPrefs: SecurityPrefs by inject()

    protected val secondPasswordHandler: SecondPasswordHandler by injectActivity()

    protected abstract val alwaysDisableScreenshots: Boolean
    private val enableScreenshots: Boolean
        get() = securityPrefs.areScreenshotsEnabled && !alwaysDisableScreenshots

    protected open val enableLogoutTimer: Boolean = true

    private var alertDialog: AlertDialog? = null
        @UiThread
        set(dlg) {
            if (!(isFinishing || isDestroyed)) { // Prevent Not Attached To Window crash
                alertDialog?.dismiss() // Prevent multiple popups
            }
            field = dlg
        }
        @UiThread
        get() = field

    private var progressDialog: MaterialProgressDialog? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenOrientation()
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected open fun lockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        stopLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityResumed()

        if (enableScreenshots) {
            enableScreenshots()
        } else {
            disallowScreenshots()
        }
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        startLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityPaused()
    }

    private fun startLogoutTimer() {
        if (enableLogoutTimer) {
            logoutTimer.start()
        }
    }

    private fun stopLogoutTimer() = logoutTimer.stop()

    private fun disallowScreenshots() =
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

    private fun enableScreenshots() =
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    // Test for screen overlays before user creates a new wallet or enters confidential information
    override fun dispatchTouchEvent(event: MotionEvent): Boolean =
        detectObscuredWindow(event) || super.dispatchTouchEvent(event)

    // Detect if touch events are being obscured by hidden overlays - These could be used for tapjacking
    // There is a possible problem, here, in that once overlays have been accepted, new apps could install
    // an untrusted overlay. TODO: Think about how best to deal with this
    private fun detectObscuredWindow(event: MotionEvent): Boolean {
        if (!securityPrefs.trustScreenOverlay && event.isObscuredTouch()) {
            showAlert(overlayAlertDlg())
            return true
        }
        return false
    }

    private fun overlayAlertDlg() =
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.screen_overlay_warning)
            .setMessage(R.string.screen_overlay_note)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ ->
                securityPrefs.trustScreenOverlay = true //
            }
            .setNegativeButton(R.string.exit) { _, _ -> this.finish() }
            .create()

    @UiThread
    protected fun showAlert(dlg: AlertDialog) {
        alertDialog = dlg
        alertDialog?.show()
    }

    @UiThread
    protected fun clearAlert() {
        alertDialog = null
    }

    @UiThread
    fun showProgressDialog(@StringRes messageId: Int, onCancel: (() -> Unit)? = null) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(onCancel != null)
            setMessage(getString(messageId))
            onCancel?.let { setOnCancelListener(it) }
            if (!isFinishing) show()
        }
    }

    @UiThread
    fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    @UiThread
    fun updateProgressDialog(msg: String) {
        progressDialog?.setMessage(msg)
    }
}

private fun MotionEvent.isObscuredTouch() = (flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0)