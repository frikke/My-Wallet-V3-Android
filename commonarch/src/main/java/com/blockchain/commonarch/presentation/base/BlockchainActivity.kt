package com.blockchain.commonarch.presentation.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.blockchain.analytics.Analytics
import com.blockchain.auth.LogoutTimer
import com.blockchain.commonarch.R
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.instrumentation.InstrumentationScaffold // ktlint-disable instrumentation-ruleset:no-instrumentation-import
import com.blockchain.koin.payloadScope
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.SecurityPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.theme.Theme
import com.blockchain.theme.ThemeService
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */

interface ManifestLauncherActivity

abstract class BlockchainActivity : ToolBarActivity() {

    private val securityPrefs: SecurityPrefs by inject()

    val analytics: Analytics by inject()

    val appUtil: AppUtilAPI by inject()

    val environment: EnvironmentConfig by inject()
    private val logoutTimer: LogoutTimer by inject()
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

    // //////////////////////////////////
    // statusbar color
    protected open val statusbarColor: ModeBackgroundColor = ModeBackgroundColor.Current

    private fun WalletMode.statusbarBg() = when (this) {
        WalletMode.CUSTODIAL -> R.drawable.custodial_bg
        WalletMode.NON_CUSTODIAL -> R.drawable.defi_bg
    }
    // //////////////////////////////////
    // //////////////////////////////////
    // theme
    private val themeService: ThemeService by inject()
    // //////////////////////////////////

    private val authPrefs: AuthPrefs by inject()
    private val walletPrefs: WalletStatusPrefs by inject()
    private val walletModeService = payloadScope.get<WalletModeService>()

    var processDeathOccurredAndThisIsNotLauncherActivity: Boolean = false
        private set

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeService.setTheme(Theme.LightMode)

        processDeathOccurredAndThisIsNotLauncherActivity =
            isFirstActivityToBeCreated && this !is ManifestLauncherActivity

        isFirstActivityToBeCreated = false
        if (processDeathOccurredAndThisIsNotLauncherActivity) {
            lifecycleScope.cancel()
            appUtil.restartApp()
            return
        }

        setStatusBarForMode()

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

    private fun setStatusBarForMode() {
        lifecycleScope.launch {
            val background: Int? = when (statusbarColor) {
                ModeBackgroundColor.Current -> {
                    val isLoggedIn = authPrefs.run { walletGuid.isNotEmpty() && pinId.isNotEmpty() } &&
                        walletPrefs.isAppUnlocked

                    if (isLoggedIn) {
                        walletModeService.walletMode.map { it.statusbarBg() }.first()
                    } else {
                        null
                    }
                }
                is ModeBackgroundColor.Override -> {
                    (statusbarColor as ModeBackgroundColor.Override).walletMode.statusbarBg()
                }
                ModeBackgroundColor.None -> {
                    null
                }
            }

            background?.let {
                with(window) {
                    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    statusBarColor = ContextCompat.getColor(this@BlockchainActivity, android.R.color.transparent)

                    val rectangle = Rect()
                    decorView.getWindowVisibleDisplayFrame(rectangle)

                    val backgroundDrawable = ContextCompat.getDrawable(this@BlockchainActivity, it)
                    backgroundDrawable?.setBounds(0, 0, rectangle.right, rectangle.bottom)
                    setBackgroundDrawable(backgroundDrawable)
                }
            }
        }
    }

    override fun setContentView(view: View) {
        if (processDeathOccurredAndThisIsNotLauncherActivity) {
            super.setContentView(createWrapperAndHideViewWithWhiteScrim(view))
            return
        }

        val view = if (true) {
            val wrapper = FrameLayout(this).apply {
                val params =
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                layoutParams = params
            }
            val composeView = ComposeView(this).apply {
                setContent {
                    InstrumentationScaffold {
                    }
                }
            }
            wrapper.addView(view)
            wrapper.addView(composeView)
            wrapper
        } else {
            view
        }
        super.setContentView(view)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        if (processDeathOccurredAndThisIsNotLauncherActivity) {
            super.setContentView(createWrapperAndHideViewWithWhiteScrim(view))
        } else {
            super.setContentView(view, params)
        }
    }

    private fun createWrapperAndHideViewWithWhiteScrim(view: View): View {
        val wrapper = FrameLayout(this).apply {
            val params =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = params
        }
        val scrimView = View(this).apply {
            val params =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = params
            setBackgroundColor(getColor(com.blockchain.componentlib.R.color.background))
        }
        view.visibility = View.GONE
        wrapper.addView(view)
        wrapper.addView(scrimView)
        return wrapper
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    protected open fun lockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun updateToolbar(
        toolbarTitle: String = "",
        menuItems: List<NavigationBarButton>? = null,
        backAction: (() -> Unit)? = null
    ) {
        updateToolbarTitle(toolbarTitle)
        menuItems?.let { items ->
            updateToolbarMenuItems(items)
        }
        backAction?.let { action ->
            updateToolbarBackAction { action() }
        }
        setStatusBarForMode()
    }

    /**
     * @param mutedBackground false to keep white bg for screens that will remain white (settings..etc)
     */
    fun updateToolbarBackground(
        modeColor: ModeBackgroundColor = ModeBackgroundColor.Current,
        mutedBackground: Boolean = true
    ) {
        toolbarBinding?.navigationToolbar?.apply {
            this.modeColor = modeColor
            this.mutedBackground = mutedBackground
        }
        setStatusBarForMode()
    }

    fun updateToolbarTitle(title: String) {
        toolbarBinding?.navigationToolbar?.title = title
        setStatusBarForMode()
    }

    fun updateToolbarMenuItems(menuItems: List<NavigationBarButton>) {
        toolbarBinding?.navigationToolbar?.endNavigationBarButtons = menuItems
        setStatusBarForMode()
    }

    fun updateToolbarBackAction(backAction: (() -> Unit)?) {
        toolbarBinding?.navigationToolbar?.onBackButtonClick = backAction
        setStatusBarForMode()
    }

    fun updateToolbarStartItem(startItem: NavigationBarButton) {
        toolbarBinding?.navigationToolbar?.startNavigationButton = startItem
        setStatusBarForMode()
    }

    fun updateToolbarIcon(icon: StackedIcon) {
        toolbarBinding?.navigationToolbar?.icon = icon
        setStatusBarForMode()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        if (processDeathOccurredAndThisIsNotLauncherActivity) return
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
        if (processDeathOccurredAndThisIsNotLauncherActivity) return
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
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.screen_overlay_warning)
            .setMessage(com.blockchain.stringResources.R.string.screen_overlay_note)
            .setCancelable(false)
            .setPositiveButton(com.blockchain.stringResources.R.string.dialog_continue) { _, _ ->
                securityPrefs.trustScreenOverlay = true
            }
            .setNegativeButton(com.blockchain.stringResources.R.string.exit) { _, _ -> this.finish() }
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

        private var isFirstActivityToBeCreated = true
    }
}

private fun MotionEvent.isObscuredTouch() = (flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0)

fun BlockchainActivity.setContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    if (true) {
        (this as ComponentActivity).setContent(parent) {
            InstrumentationScaffold {
                content()
            }
        }
    } else {
        (this as ComponentActivity).setContent(parent, content)
    }
}
