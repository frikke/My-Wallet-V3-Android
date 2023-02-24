package piuk.blockchain.android.ui.login.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.text.method.LinkMovementMethod
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.core.auth.isValidGuid
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.presentation.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityLoginAuthBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.customersupport.CustomerSupportAnalytics
import piuk.blockchain.android.ui.customersupport.CustomerSupportSheet
import piuk.blockchain.android.ui.login.LoginAnalytics
import piuk.blockchain.android.ui.login.PayloadHandler
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.ui.recover.AccountRecoveryActivity
import piuk.blockchain.android.ui.settings.SettingsAnalytics
import piuk.blockchain.android.ui.settings.SettingsAnalytics.Companion.TWO_SET_MOBILE_NUMBER_OPTION
import piuk.blockchain.android.ui.settings.security.pin.PinActivity
import piuk.blockchain.android.ui.start.ManualPairingActivity
import piuk.blockchain.android.urllinks.RESET_2FA
import piuk.blockchain.android.urllinks.SECOND_PASSWORD_EXPLANATION
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.clearErrorState
import piuk.blockchain.android.util.setErrorState
import timber.log.Timber

class LoginAuthActivity :
    MviActivity<LoginAuthModel, LoginAuthIntents, LoginAuthState, ActivityLoginAuthBinding>() {

    override val alwaysDisableScreenshots: Boolean
        get() = true

    override val model: LoginAuthModel by scopedInject()

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val remoteLogger: RemoteLogger by inject()
    private val walletPrefs: WalletStatusPrefs by inject()
    private val fraudService: FraudService by inject()

    private lateinit var currentState: LoginAuthState

    private val pollingPayload: LoginAuthInfo.ExtendedAccountInfo? by lazy {
        intent.getSerializableExtra(POLLING_PAYLOAD) as? LoginAuthInfo.ExtendedAccountInfo
    }

    private val base64EncodedPayload: String by lazy {
        intent.getStringExtra(BASE_64_ENCODED_PAYLOAD).orEmpty()
    }

    private var email: String = ""
    private var userId: String = ""
    private var recoveryToken: String = ""
    private val compositeDisposable = CompositeDisposable()

    private val isTwoFATimerRunning = AtomicBoolean(false)
    private val twoFATimer by lazy {
        object : CountDownTimer(TWO_FA_COUNTDOWN, TWO_FA_STEP) {
            override fun onTick(millisUntilFinished: Long) {
                isTwoFATimerRunning.set(true)
            }

            override fun onFinish() {
                isTwoFATimerRunning.set(false)
                model.process(LoginAuthIntents.Reset2FARetries)
            }
        }
    }

    private val analyticsInfo: LoginAuthInfo?
        get() = if (::currentState.isInitialized) currentState.authInfoForAnalytics else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar()
        initControls()
    }

    override fun onStart() {
        super.onStart()
        processIntentData()
    }

    override fun onStop() {
        compositeDisposable.clear()
        twoFATimer.cancel()
        super.onStop()
    }

    private fun processIntentData() {
        analytics.logEvent(LoginAnalytics.DeviceVerified(analyticsInfo))

        val payload = pollingPayload
        val data = PayloadHandler.getDataFromIntent(intent)

        // data from the intent is either a GUID or a base64 from deep-linking that we need to decode.
        val loginAuthIntent = when {
            payload != null -> LoginAuthIntents.GetSessionId(payload)
            data == null -> LoginAuthIntents.ShowAuthRequired
            data.isValidGuid() -> LoginAuthIntents.ShowManualPairing(data)
            else -> decodeBase64PayloadToIntent(data)
        }
        model.process(loginAuthIntent)
    }

    private fun decodeBase64PayloadToIntent(data: String) =
        try {
            val json = PayloadHandler.decodeToJsonString(data)
            LoginAuthIntents.InitLoginAuthInfo(json)
        } catch (ex: Exception) {
            Timber.e(ex)
            remoteLogger.logException(ex)
            // Fall back to legacy manual pairing
            LoginAuthIntents.ShowManualPairing(null)
        }

    private fun initControls() {
        with(binding) {
            passwordText.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    passwordTextLayout.clearErrorState()
                }
            })
            codeText.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(s: Editable) {
                    codeTextLayout.clearErrorState()
                }
            })
            forgotPasswordButton.setOnClickListener {
                launchPasswordRecoveryFlow()
            }

            continueButton.setOnClickListener {
                analytics.logEvent(LoginAnalytics.LoginPasswordEntered(analyticsInfo))
                if (currentState.authMethod != TwoFAMethod.OFF) {
                    model.process(
                        LoginAuthIntents.SubmitTwoFactorCode(
                            password = passwordText.text.toString(),
                            code = codeText.text.toString()
                        )
                    )
                    analytics.logEvent(LoginAnalytics.LoginTwoFaEntered(analyticsInfo))
                    analytics.logEvent(SettingsAnalytics.TwoStepVerificationCodeSubmitted(TWO_SET_MOBILE_NUMBER_OPTION))
                } else {
                    model.process(LoginAuthIntents.VerifyPassword(passwordText.text.toString()))
                }
            }

            twoFaResend.text = getString(R.string.two_factor_resend_sms, walletPrefs.resendSmsRetries)
            twoFaResend.setOnClickListener {
                if (!isTwoFATimerRunning.get()) {
                    model.process(LoginAuthIntents.RequestNew2FaCode)
                } else {
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.two_factor_retries_exceeded),
                        type = SnackbarType.Error
                    ).show()
                }
            }
        }
    }

    override fun initBinding(): ActivityLoginAuthBinding = ActivityLoginAuthBinding.inflate(layoutInflater)

    private fun setupToolbar() {
        updateToolbar(
            toolbarTitle = getString(R.string.login_title),
            backAction = { clearKeyboardAndFinish() }
        )

        updateToolbarMenuItems(
            listOf(
                NavigationBarButton.Icon(
                    drawable = R.drawable.ic_question,
                    contentDescription = R.string.accessibility_support
                ) {
                    analytics.logEvent(CustomerSupportAnalytics.CustomerSupportClicked)
                    showCustomerSupportSheet()
                }
            )
        )
    }

    override fun render(newState: LoginAuthState) {
        renderAuthStatus(newState)
        updateLoginData(newState)
        update2FALayout(newState.authMethod)

        newState.twoFaState?.let {
            renderRemainingTries(it)
        }

        currentState = newState
    }

    private fun renderAuthStatus(newState: LoginAuthState) {
        when (newState.authStatus) {
            AuthStatus.None,
            AuthStatus.InitAuthInfo -> binding.progressBar.visible()
            AuthStatus.GetSessionId,
            AuthStatus.AuthorizeApproval,
            AuthStatus.GetPayload -> binding.progressBar.gone()
            AuthStatus.Submit2FA,
            AuthStatus.VerifyPassword,
            AuthStatus.UpdateMobileSetup -> binding.progressBar.visible()
            AuthStatus.Complete -> {
                analytics.logEvent(LoginAnalytics.LoginRequestApproved(analyticsInfo))
                startActivity(
                    PinActivity.newIntent(
                        context = this,
                        startForResult = false,
                        originScreen = PinActivity.Companion.OriginScreenToPin.LOGIN_AUTH_SCREEN,
                        addFlagsToClear = true
                    )
                )
                null
            }
            AuthStatus.PairingFailed -> showErrorSnackbar(R.string.pairing_failed)
            AuthStatus.InvalidPassword -> {
                analytics.logEvent(LoginAnalytics.LoginPasswordDenied(analyticsInfo))
                binding.progressBar.gone()
                binding.passwordTextLayout.setErrorState(getString(R.string.invalid_password))
            }
            AuthStatus.AuthFailed -> {
                analytics.logEvent(LoginAnalytics.LoginRequestDenied(analyticsInfo))
                showErrorSnackbar(R.string.auth_failed)
            }
            AuthStatus.InitialError -> showErrorSnackbar(R.string.common_error)
            AuthStatus.AuthRequired -> showSnackbar(getString(R.string.auth_required))
            AuthStatus.Invalid2FACode -> {
                analytics.logEvent(LoginAnalytics.LoginTwoFaDenied(analyticsInfo))
                binding.progressBar.gone()
                binding.codeTextLayout.setErrorState(getString(R.string.invalid_two_fa_code))
            }
            AuthStatus.ShowManualPairing -> {
                startActivity(ManualPairingActivity.newInstance(this, newState.guid))
                clearKeyboardAndFinish()
            }
            AuthStatus.AccountLocked -> {
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.account_locked_title)
                    .setMessage(R.string.account_locked_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.common_go_back) { _, _ ->
                        clearKeyboardAndFinish()
                        fraudService.endFlow(FraudFlow.LOGIN)
                    }
                    .create()
                    .show()
            }
        }.exhaustive
    }

    private fun clearKeyboardAndFinish() {
        hideKeyboard()
        finish()
    }

    private fun renderRemainingTries(state: TwoFaCodeState) =
        when (state) {
            is TwoFaCodeState.TwoFaRemainingTries ->
                binding.twoFaResend.text = getString(R.string.two_factor_resend_sms, state.remainingRetries)
            is TwoFaCodeState.TwoFaTimeLock -> {
                if (!isTwoFATimerRunning.get()) {
                    twoFATimer.start()
                    BlockchainSnackbar.make(
                        binding.root,
                        getString(R.string.two_factor_retries_exceeded),
                        type = SnackbarType.Error
                    ).show()
                }
                binding.twoFaResend.text = getString(R.string.two_factor_resend_sms, 0)
            }
        }

    private fun updateLoginData(currentState: LoginAuthState) {
        userId = currentState.userId
        email = currentState.email
        recoveryToken = currentState.recoveryToken
        with(binding) {
            loginEmailText.setText(email)
            loginWalletLabel.text = getString(R.string.login_wallet_id_text, currentState.guid)
            forgotPasswordButton.isEnabled = email.isNotEmpty()
        }
    }

    private fun update2FALayout(authMethod: TwoFAMethod) {
        with(binding) {
            when (authMethod) {
                TwoFAMethod.OFF -> codeTextLayout.gone()
                TwoFAMethod.YUBI_KEY -> {
                    codeTextLayout.visible()
                    codeTextLayout.hint = getString(R.string.hardware_key_hint)
                    codeText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    codeLabel.visible()
                    codeLabel.text = getString(R.string.tap_hardware_key_label)
                }
                TwoFAMethod.SMS -> {
                    codeTextLayout.visible()
                    codeTextLayout.hint = getString(R.string.two_factor_code_hint)
                    twoFaResend.visible()
                    setup2FANotice(
                        textId = R.string.lost_2fa_notice,
                        annotationForLink = RESET_2FA_LINK_ANNOTATION,
                        url = RESET_2FA
                    )
                }
                TwoFAMethod.GOOGLE_AUTHENTICATOR -> {
                    codeTextLayout.visible()
                    codeTextLayout.hint = getString(R.string.two_factor_code_hint)
                    codeText.inputType = InputType.TYPE_NUMBER_VARIATION_NORMAL
                    codeText.keyListener = DigitsKeyListener.getInstance(DIGITS)
                    setup2FANotice(
                        textId = R.string.lost_2fa_notice,
                        annotationForLink = RESET_2FA_LINK_ANNOTATION,
                        url = RESET_2FA
                    )
                }
                TwoFAMethod.SECOND_PASSWORD -> {
                    codeTextLayout.visible()
                    codeTextLayout.hint = getString(R.string.second_password_hint)
                    forgotSecondPasswordButton.visible()
                    forgotSecondPasswordButton.setOnClickListener { launchPasswordRecoveryFlow() }
                    setup2FANotice(
                        textId = R.string.second_password_notice,
                        annotationForLink = SECOND_PASSWORD_LINK_ANNOTATION,
                        url = SECOND_PASSWORD_EXPLANATION
                    )
                }
            }.exhaustive
            continueButton.setOnClickListener {
                if (authMethod != TwoFAMethod.OFF) {
                    model.process(
                        LoginAuthIntents.SubmitTwoFactorCode(
                            password = passwordText.text.toString(),
                            code = codeText.text.toString().trim()
                        )
                    )
                    analytics.logEvent(SettingsAnalytics.TwoStepVerificationCodeSubmitted(TWO_SET_MOBILE_NUMBER_OPTION))
                } else {
                    model.process(LoginAuthIntents.VerifyPassword(passwordText.text.toString()))
                }
            }
        }
    }

    private fun setup2FANotice(@StringRes textId: Int, annotationForLink: String, url: String) {
        binding.twoFaNotice.apply {
            visible()
            val links = mapOf(annotationForLink to Uri.parse(url))
            text = StringUtils.getStringWithMappedAnnotations(context, textId, links) {
                analytics.logEvent(LoginAnalytics.LoginLearnMoreClicked(analyticsInfo))
            }
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun showErrorSnackbar(@StringRes message: Int) {
        binding.progressBar.gone()
        binding.passwordText.setText("")
        BlockchainSnackbar.make(binding.root, getString(message), type = SnackbarType.Error).show()
    }

    private fun showSnackbar(message: String) {
        binding.progressBar.gone()
        binding.passwordText.setText("")
        BlockchainSnackbar.make(binding.root, message).show()
    }

    private fun launchPasswordRecoveryFlow() {
        analytics.logEvent(LoginAnalytics.LoginHelpClicked(analyticsInfo))
        val intent = Intent(this, AccountRecoveryActivity::class.java).apply {
            putExtra(EMAIL, email)
            putExtra(USER_ID, userId)
            putExtra(RECOVERY_TOKEN, recoveryToken)
        }
        startActivity(intent)
    }

    private fun showCustomerSupportSheet() {
        showBottomSheet(CustomerSupportSheet.newInstance())
    }

    companion object {
        fun newInstance(
            context: Activity,
            pollingPayload: LoginAuthInfo.ExtendedAccountInfo,
            base64EncodedPayload: String
        ): Intent =
            Intent(context, LoginAuthActivity::class.java).apply {
                putExtra(POLLING_PAYLOAD, pollingPayload)
                putExtra(BASE_64_ENCODED_PAYLOAD, base64EncodedPayload)
            }

        const val LINK_DELIMITER = "/login/"
        private const val POLLING_PAYLOAD = "POLLING_PAYLOAD"
        private const val BASE_64_ENCODED_PAYLOAD = "BASE_64_ENCODED_PAYLOAD"
        private const val EMAIL = "email"
        private const val USER_ID = "user_id"
        private const val RECOVERY_TOKEN = "recovery_token"
        private const val DIGITS = "1234567890"
        private const val SECOND_PASSWORD_LINK_ANNOTATION = "learn_more"
        private const val RESET_2FA_LINK_ANNOTATION = "reset_2fa"
    }
}
