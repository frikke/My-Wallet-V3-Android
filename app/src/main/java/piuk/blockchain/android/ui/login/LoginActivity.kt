package piuk.blockchain.android.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.customerSupportSheetFeatureFlag
import com.blockchain.koin.scopedInject
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityLoginBinding
import piuk.blockchain.android.ui.customersupport.CustomerSupportAnalytics
import piuk.blockchain.android.ui.customersupport.CustomerSupportSheet
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.login.auth.LoginAuthActivity
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.ui.start.ManualPairingActivity
import piuk.blockchain.android.util.AfterTextChangedWatcher
import timber.log.Timber

class LoginActivity :
    LoginIntentCoordinator, MviActivity<LoginModel, LoginIntents, LoginState, ActivityLoginBinding>() {

    override val model: LoginModel by scopedInject()

    override val alwaysDisableScreenshots: Boolean = true

    private val environmentConfig: EnvironmentConfig by inject()

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()

    private val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(this, gso)
    }

    private val recaptchaClient: GoogleReCaptchaClient by lazy {
        GoogleReCaptchaClient(this, environmentConfig)
    }

    private val customerSupportSheetFF: FeatureFlag by inject(customerSupportSheetFeatureFlag)

    private var state: LoginState? = null

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        recaptchaClient.initReCaptcha()
        checkExistingSessionOrDeeplink(intent)
    }

    private fun checkExistingSessionOrDeeplink(intent: Intent) {
        val action = intent.action
        val data = intent.data
        if (action != null && data != null) {
            model.process(LoginIntents.CheckForExistingSessionOrDeepLink(action, data))
        }
    }

    override fun onStart() {
        super.onStart()

        analytics.logEvent(LoginAnalytics.LoginViewed)
        with(binding) {
            loginEmailText.apply {
                inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

                addTextChangedListener(object : AfterTextChangedWatcher() {
                    override fun afterTextChanged(s: Editable) {
                        model.process(LoginIntents.UpdateEmail(s.toString()))
                    }
                })

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_GO && continueButton.isEnabled) {
                        onContinueButtonClicked()
                    }
                    true
                }
            }
            continueButton.setOnClickListener {
                onContinueButtonClicked()
            }

            continueWithGoogleButton.setOnClickListener {
                analytics.logEvent(LoginAnalytics.LoginWithGoogleMethodSelected)
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            }
            if (environmentConfig.isRunningInDebugMode()) {
                scanPairingButton.setOnClickListener {
                    QrScanActivity.start(this@LoginActivity, QrExpected.MAIN_ACTIVITY_QR)
                }
                scanPairingButton.visibleIf {
                    environmentConfig.environment != Environment.PRODUCTION
                }
                manualPairingButton.apply {
                    setOnClickListener {
                        startActivity(Intent(context, ManualPairingActivity::class.java))
                    }
                    isEnabled = true
                    visible()
                }
            }
        }
    }

    private fun onContinueButtonClicked() {
        binding.loginEmailText.text?.let { emailInputText ->
            if (emailInputText.isNotBlank()) {
                if (isDemoAccount(emailInputText.toString().trim())) {
                    val intent = ManualPairingActivity.newInstance(this, BuildConfig.PLAY_STORE_DEMO_WALLET_ID)
                    startActivity(intent)
                } else {
                    this@LoginActivity.hideKeyboard()
                    verifyReCaptcha(emailInputText.toString())
                }
            }
        }
    }

    private fun isDemoAccount(email: String): Boolean = email == BuildConfig.PLAY_STORE_DEMO_EMAIL

    override fun onResume() {
        super.onResume()
        model.process(LoginIntents.ResumePolling)
    }

    override fun onPause() {
        model.process(LoginIntents.CancelPolling)
        super.onPause()
    }

    override fun onBackPressed() {
        model.process(LoginIntents.ResetState)
        super.onBackPressed()
    }

    override fun onDestroy() {
        recaptchaClient.close()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkExistingSessionOrDeeplink(intent)
    }

    override fun initBinding(): ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.login_title),
            backAction = { onBackPressed() }
        )

        customerSupportSheetFF.enabled.onErrorReturn { false }.subscribe { enabled ->
            if (enabled) {
                updateToolbarMenuItems(
                    listOf(
                        NavigationBarButton.Icon(R.drawable.ic_question) {
                            analytics.logEvent(CustomerSupportAnalytics.CustomerSupportClicked)
                            showCustomerSupportSheet()
                        }
                    )
                )
            }
        }
    }

    override fun process(intent: LoginIntents) = model.process(intent)

    override fun render(newState: LoginState) {
        updateUI(newState)
        if (state?.currentStep == newState.currentStep) {
            return
        }
        state = newState
        when (newState.currentStep) {
            LoginStep.SHOW_SCAN_ERROR -> {
                showSnackbar(SnackbarType.Error, R.string.pairing_failed)
                if (newState.shouldRestartApp) {
                    restartToLauncherActivity()
                }
            }
            LoginStep.ENTER_PIN -> {
                showLoginApprovalStatePrompt(newState.loginApprovalState)
                startActivity(
                    PinActivity.newIntent(
                        context = this,
                        startForResult = false,
                        originScreen = PinActivity.Companion.OriginScreenToPin.LOGIN_SCREEN,
                        addFlagsToClear = true,
                    )
                )
            }
            LoginStep.VERIFY_DEVICE -> navigateToVerifyDevice(newState)
            LoginStep.SHOW_SESSION_ERROR -> showSnackbar(SnackbarType.Error, R.string.login_failed_session_id_error)
            LoginStep.SHOW_EMAIL_ERROR -> showSnackbar(SnackbarType.Error, R.string.login_send_email_error)
            LoginStep.NAVIGATE_FROM_DEEPLINK -> {
                newState.intentUri?.let { uri ->
                    startActivity(Intent(newState.intentAction, uri, this, LoginAuthActivity::class.java))
                }
            }
            LoginStep.NAVIGATE_FROM_PAYLOAD -> {
                newState.payload?.let {
                    startActivity(LoginAuthActivity.newInstance(this, it, newState.payloadBase64String))
                    model.process(LoginIntents.ShowEmailSent)
                }
            }
            LoginStep.UNKNOWN_ERROR -> {
                model.process(LoginIntents.CheckShouldNavigateToOtherScreen)
                showSnackbar(SnackbarType.Error, R.string.common_error)
            }
            LoginStep.POLLING_PAYLOAD_ERROR -> handlePollingError(newState.pollingState)
            LoginStep.ENTER_EMAIL -> returnToEmailInput()
            // TODO AND-5317 this should display a bottom sheet with info about what device we're authorising
            LoginStep.REQUEST_APPROVAL -> showLoginApprovalDialog()
            LoginStep.NAVIGATE_TO_LANDING_PAGE -> {
                showLoginApprovalStatePrompt(newState.loginApprovalState)
                model.process(LoginIntents.ResetState)
                restartToLauncherActivity()
                finish()
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun showLoginApprovalStatePrompt(loginApprovalState: LoginApprovalState) =
        when (loginApprovalState) {
            LoginApprovalState.NONE -> {
                // do nothing
            }
            LoginApprovalState.APPROVED -> showSnackbar(SnackbarType.Success, R.string.login_approved_toast)
            LoginApprovalState.REJECTED -> showSnackbar(SnackbarType.Error, R.string.login_denied_toast)
        }

    private fun restartToLauncherActivity() {
        startActivity(
            Intent(this, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun showLoginApprovalDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.login_approval_dialog_title)
            .setMessage(R.string.login_approval_dialog_message)
            .setPositiveButton(R.string.common_approve) { di, _ ->
                model.process(LoginIntents.ApproveLoginRequest)
                di.dismiss()
            }
            .setNegativeButton(R.string.common_deny) { di, _ ->
                model.process(LoginIntents.DenyLoginRequest)
                di.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun handlePollingError(state: AuthPollingState) =
        when (state) {
            AuthPollingState.TIMEOUT -> {
                showSnackbar(SnackbarType.Error, R.string.login_polling_timeout)
                returnToEmailInput()
            }
            AuthPollingState.ERROR -> {
                // fail silently? - maybe log analytics
            }
            AuthPollingState.DENIED -> {
                showSnackbar(SnackbarType.Error, R.string.login_polling_denied)
                returnToEmailInput()
            }
            else -> {
                // no error, do nothing
            }
        }

    private fun showSnackbar(type: SnackbarType, @StringRes message: Int) =
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = type
        ).show()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == QrScanActivity.SCAN_URI_RESULT) {
            data.getRawScanData()?.let { rawQrString ->
                model.process(LoginIntents.LoginWithQr(rawQrString))
            }
        } else if (resultCode == RESULT_OK && requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.result.email?.let { email ->
                    verifyReCaptcha(email)
                } ?: showSnackbar(SnackbarType.Info, R.string.login_google_email_not_found)
            } catch (apiException: ApiException) {
                Timber.e(apiException)
                showSnackbar(SnackbarType.Error, R.string.login_google_sign_in_failed)
            }
        }
    }

    private fun returnToEmailInput() {
        supportFragmentManager.run {
            fragments.forEach { fragment ->
                beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
        model.process(LoginIntents.RevertToEmailInput)
    }

    private fun updateUI(newState: LoginState) {
        with(binding) {
            progressBar.visibleIf { newState.isLoading }
            // TODO enable Google auth once ready along with the OR label
            continueButton.visibleIf {
                newState.isTypingEmail ||
                    newState.currentStep == LoginStep.SEND_EMAIL ||
                    newState.currentStep == LoginStep.VERIFY_DEVICE
            }
            continueButton.isEnabled =
                emailRegex.matches(newState.email) || (newState.isTypingEmail && emailRegex.matches(newState.email))
        }
    }

    private fun navigateToVerifyDevice(newState: LoginState) {
        supportFragmentManager.run {
            beginTransaction()
                .replace(
                    R.id.content_frame,
                    VerifyDeviceFragment.newInstance(
                        newState.sessionId, newState.email, newState.captcha
                    ),
                    VerifyDeviceFragment::class.simpleName
                )
                .addToBackStack(VerifyDeviceFragment::class.simpleName)
                .commitAllowingStateLoss()
        }
    }

    private fun verifyReCaptcha(selectedEmail: String) {
        recaptchaClient.verifyForLogin(
            onSuccess = { response ->
                analytics.logEvent(LoginAnalytics.LoginIdentifierEntered)
                model.process(
                    LoginIntents.ObtainSessionIdForEmail(
                        selectedEmail = selectedEmail,
                        captcha = response.tokenResult
                    )
                )
            },
            onError = { showSnackbar(SnackbarType.Error, R.string.common_error) }
        )
    }

    private fun showCustomerSupportSheet() {
        showBottomSheet(CustomerSupportSheet.newInstance())
    }

    private val emailRegex = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    )

    companion object {
        private const val RC_SIGN_IN = 10
    }
}
