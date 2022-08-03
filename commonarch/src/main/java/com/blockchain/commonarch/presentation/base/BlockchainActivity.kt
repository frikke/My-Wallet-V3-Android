package com.blockchain.commonarch.presentation.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.blockchain.analytics.Analytics
import com.blockchain.auth.LogoutTimer
import com.blockchain.commonarch.R
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.SecurityPrefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.android.ext.android.inject

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */

abstract class BlockchainActivity : ToolBarActivity() {

    private val securityPrefs: SecurityPrefs by inject()

    val analytics: Analytics by inject()

    val appUtil: AppUtilAPI by inject()

    val environment: EnvironmentConfig by inject()
    val logoutTimer: LogoutTimer by inject()
    private val remoteLogger: RemoteLogger by inject()

    protected abstract val alwaysDisableScreenshots: Boolean
    protected open val toolbarBinding: ToolbarGeneralBinding?
        get() = null

    private val activityIndicator = ActivityIndicator()
    private val compositeDisposable = CompositeDisposable()

    private val enableScreenshots: Boolean
        get() = environment.isRunningInDebugMode() ||
            (securityPrefs.areScreenshotsEnabled && !alwaysDisableScreenshots) ||
            environment.isCompanyInternalBuild()

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
        get

    private var progressDialog: MaterialProgressDialog? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenOrientation()

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fragmentManager: FragmentManager, fragment: Fragment) {
                    super.onFragmentResumed(fragmentManager, fragment)
                    remoteLogger.logView(fragment::class.java.name)
                }
            },
            true
        )
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected open fun lockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun updateToolbar(
        toolbarTitle: String = "",
        menuItems: List<NavigationBarButton>? = null,
        backAction: (() -> Unit)? = null,
    ) {
        updateToolbarTitle(toolbarTitle)
        menuItems?.let { items ->
            updateToolbarMenuItems(items)
        }
        backAction?.let { action ->
            updateToolbarBackAction { action() }
        }
    }

    fun updateToolbarTitle(title: String) {
        toolbarBinding?.navigationToolbar?.title = title
    }

    fun updateToolbarMenuItems(menuItems: List<NavigationBarButton>) {
        toolbarBinding?.navigationToolbar?.endNavigationBarButtons = menuItems
    }

    fun updateToolbarBackAction(backAction: (() -> Unit)?) {
        toolbarBinding?.navigationToolbar?.onBackButtonClick = backAction
    }

    fun updateToolbarStartItem(startItem: NavigationBarButton) {
        toolbarBinding?.navigationToolbar?.startNavigationButton = startItem
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        logoutTimer.stop()

        if (enableScreenshots) {
            enableScreenshots()
        } else {
            disallowScreenshots()
        }
        appUtil.activityIndicator = activityIndicator

        compositeDisposable += activityIndicator.loading
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                if (it == true) {
                    showLoading()
                } else {
                    hideLoading()
                }
            }
    }

    open fun showLoading() {}
    open fun hideLoading() {}

    @CallSuper
    override fun onPause() {
        super.onPause()
        if (enableLogoutTimer) {
            logoutTimer.start()
        }
        compositeDisposable.clear()
    }

    private fun disallowScreenshots() =
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

    private fun enableScreenshots() =
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

    // Test for screen overlays before user creates a new wallet or enters confidential information
    override fun dispatchTouchEvent(event: MotionEvent): Boolean =
        detectObscuredWindow(event) || super.dispatchTouchEvent(event)

    // Detect if touch events are being obscured by hidden overlays - These could be used for tapjacking
    // There is a possible problem, here, in that once overlays have been accepted, new apps could install
    // an untrusted overlay.
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
                securityPrefs.trustScreenOverlay = true
            }
            .setNegativeButton(R.string.exit) { _, _ -> this.finish() }
            .create()

    @UiThread
    fun showAlert(dlg: AlertDialog) {
        alertDialog = dlg
        alertDialog?.show()
    }

    @UiThread
    fun clearAlert() {
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
    fun updateProgressDialog(msg: String, onCancel: () -> Unit = {}, isCancelable: Boolean = false) {
    progressDialog?.apply {
        setCancelable(isCancelable)
        setMessage(msg)
        setOnCancelListener(onCancel)
    }
}

    @UiThread
    fun showBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        if (!supportFragmentManager.isDestroyed) {
            bottomSheet.show(supportFragmentManager, BOTTOM_DIALOG)
        }
    }

    @UiThread
    fun clearBottomSheet() {
        val dlg = supportFragmentManager.findFragmentByTag(BOTTOM_DIALOG)

        dlg?.let {
            (it as? SlidingModalBottomDialog<ViewBinding>)?.dismiss()
                ?: throw IllegalStateException("Fragment is not a $BOTTOM_DIALOG")
        }
    }

    @UiThread
    fun replaceBottomSheet(bottomSheet: BottomSheetDialogFragment) {
        clearBottomSheet()
        showBottomSheet(bottomSheet)
    }

    companion object {
        private const val BOTTOM_DIALOG = "BOTTOM_DIALOG"
        const val LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT"
    }
}

private fun MotionEvent.isObscuredTouch() = (flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0)
