package piuk.blockchain.android.ui.settings.security.pin

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.Color
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.biometrics.BiometricAuthError
import com.blockchain.biometrics.BiometricsCallback
import com.blockchain.biometrics.BiometricsType
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.keyboard.KeyboardButton
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.viewextensions.getAlertDialogPaddedView
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.ui.password.SecondPasswordHandler
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricPromptUtil
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.data.biometrics.WalletBiometricData
import piuk.blockchain.android.databinding.ActivityPinBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.auth.MobileNoticeDialog
import piuk.blockchain.android.ui.customersupport.CustomerSupportAnalytics
import piuk.blockchain.android.ui.customersupport.CustomerSupportSheet
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.home.MobileNoticeDialogFragment
import piuk.blockchain.android.ui.launcher.loader.LoaderActivity
import piuk.blockchain.android.ui.launcher.loader.LoginMethod
import piuk.blockchain.android.ui.settings.security.biometrics.RequestBiometricsDialog
import piuk.blockchain.android.urllinks.APP_STORE_URI
import piuk.blockchain.android.urllinks.APP_STORE_URL
import piuk.blockchain.android.urllinks.WALLET_STATUS_URL
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.copyHashOnLongClick
import piuk.blockchain.android.util.scopedInjectActivity

class PinActivity :
    MviActivity<
        PinModel,
        PinIntent,
        PinState,
        ActivityPinBinding
        >(),
    RequestBiometricsDialog.Host {

    override val model: PinModel by scopedInject()
    private val fraudService: FraudService by inject()
    override fun initBinding(): ActivityPinBinding =
        ActivityPinBinding.inflate(layoutInflater)

    override val alwaysDisableScreenshots: Boolean = true

    override val statusbarColor = ModeBackgroundColor.None

    private val environmentConfig: EnvironmentConfig by inject()
    private val util: AppUtil by inject()
    private val secondPasswordDialog: SecondPasswordDialog by scopedInjectActivity()
    private val biometricsController: BiometricsController by scopedInject()
    private var isBiometricsVisible = false

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private val isForValidatingPinForResult by lazy {
        intent?.getBooleanExtra(KEY_VALIDATING_PIN_FOR_RESULT, false) ?: false
    }

    private val originScreen by lazy {
        intent?.getSerializableExtra(ORIGIN_SCREEN) as OriginScreenToPin
    }

    private val referralCode: String? by lazy {
        intent?.getStringExtra(KEY_REFERRAL_CODE)
    }

    private val isChangingPin: Boolean by lazy {
        originScreen == OriginScreenToPin.CHANGE_PIN_SECURITY
    }

    private val pinBoxList = mutableListOf<AppCompatImageView>()
    private var tempNewPin = ""
    private var pinUserInput = ""

    private lateinit var lastState: PinState
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBackPress()

        appUpdateManager = AppUpdateManagerFactory.create(this)

        trackFraud()
        setToolbar()
        init()

        with(binding) {
            pinLogout.apply {
                text = getString(com.blockchain.stringResources.R.string.logout)
                setOnClickListener { model.process(PinIntent.PinLogout) }
            }
            pinKeyboard.apply {
                bgColor = Color(getColor(com.blockchain.componentlib.R.color.backgroundSecondary))
                onClick = { button ->
                    if (::lastState.isInitialized) {
                        when (button) {
                            is KeyboardButton.Value -> {
                                pinUserInput += button.value
                                onPadClicked()
                            }

                            KeyboardButton.Backspace -> {
                                if (pinUserInput.isNotEmpty()) {
                                    pinUserInput = pinUserInput.dropLast(1)
                                    onDeleteClicked()
                                }
                            }

                            KeyboardButton.Biometrics -> {
                                model.process(PinIntent.CheckFingerprint)
                            }

                            KeyboardButton.None -> {
                                // no-op
                            }
                        }
                    }
                }
            }
            customerSupport.setOnClickListener {
                analytics.logEvent(CustomerSupportAnalytics.CustomerSupportClicked)
                fraudService.endFlow(FraudFlow.LOGIN)
                showCustomerSupportSheet()
            }
            customerSupport.visible()
        }
    }

    override fun render(newState: PinState) {
        lastState = newState
        setPinView(newState.action)

        checkFingerprintStatus()

        if (newState.pinStatus.isPinValidated) {
            when {
                isChangingPin && newState.action == PinScreenView.LoginWithPin -> {
                    model.process(PinIntent.UpdateAction(PinScreenView.CreateNewPin))
                    clearPin()
                }

                !isChangingPin -> finishWithResultOk(getIntroducedPin())
            }
        }

        // We don't seem to use this "mobile_notice" from firebase but we keep old logic in the redesign.
        if (newState.action != PinScreenView.CreateNewPin) {
            model.process(PinIntent.FetchRemoteMobileNotice)
        }

        if (newState.error != PinError.NONE) {
            errorPinBoxes()
            shakePinBoxes()
            handleErrors(newState.error)
            model.process(PinIntent.ClearStateAlreadyHandled)
        }

        newState.passwordStatus?.let {
            if (it.isPasswordValid) {
                handlePasswordValidated()
            }
            if (it.passwordError != PasswordError.NONE) {
                handlePasswordErrors(it.passwordError)
            }
            model.process(PinIntent.ClearStateAlreadyHandled)
        }

        newState.payloadStatus.let {
            if (it.isPayloadCompleted) {
                onUpdateFinished(newState.pinStatus.isFromPinCreation)
            }
            if (it.payloadError != PayloadError.NONE) {
                handlePayloadErrors(it.payloadError)
            }
            model.process(PinIntent.ClearStateAlreadyHandled)
        }

        binding.layoutWarning.root.visibleIf { !newState.isApiHealthyStatus }
        newState.showMobileNotice?.let { showMobileNotice(it) }

        newState.appUpgradeStatus.let {
            when (it.appNeedsToUpgrade) {
                UpgradeAppMethod.NONE -> {}
                UpgradeAppMethod.FLEXIBLE -> {
                    require(it.appUpdateInfo != null)
                    updateFlexibleNatively(appUpdateManager, it.appUpdateInfo)
                }

                UpgradeAppMethod.FORCED_NATIVELY -> {
                    require(it.appUpdateInfo != null)
                    updateForcedNatively(appUpdateManager, it.appUpdateInfo)
                }

                UpgradeAppMethod.FORCED_STORE -> handleForcedUpdateFromStore()
            }
        }

        newState.upgradeWalletStatus?.let {
            if (it.isWalletUpgradeRequired) {
                walletUpgradeRequired(
                    newState.passwordStatus?.passwordTriesRemaining ?: 0,
                    newState.pinStatus.isFromPinCreation
                )
            } else if (it.upgradeAppSucceeded) {
                onUpdateFinished(newState.pinStatus.isFromPinCreation)
            } else {
                onWalletUpgradeFailed()
            }
            model.process(PinIntent.ClearStateAlreadyHandled)
        }

        updateLoading(newState.isLoading)
    }

    override fun cancel() {
        finishSignupProcess()
    }

    private fun loadComposableData() {
        with(binding) {
            pinBoxList.apply {
                add(pinBox0)
                add(pinBox1)
                add(pinBox2)
                add(pinBox3)
            }
        }
    }

    private fun trackFraud() {
        when (originScreen) {
            OriginScreenToPin.CREATE_WALLET -> fraudService.trackFlow(FraudFlow.SIGNUP)
            OriginScreenToPin.LAUNCHER_SCREEN,
            OriginScreenToPin.LOADER_SCREEN,
            OriginScreenToPin.LOGIN_SCREEN,
            OriginScreenToPin.PIN_SCREEN,
            OriginScreenToPin.MANUAL_PAIRING_SCREEN,
            OriginScreenToPin.LOGIN_AUTH_SCREEN,
            OriginScreenToPin.PASSWORD_REQUIRED_SCREEN -> fraudService.trackFlow(FraudFlow.LOGIN)

            else -> {
                // NO-OP
            }
        }
    }

    private fun setToolbar() {
        updateToolbarBackground(modeColor = ModeBackgroundColor.None, mutedBackground = false)

        when (originScreen) {
            OriginScreenToPin.CHANGE_PIN_SECURITY -> {
                updateToolbar(
                    toolbarTitle = getString(com.blockchain.stringResources.R.string.pin_toolbar_change),
                    backAction = { onBackPressedDispatcher.onBackPressed() }
                )
            }

            OriginScreenToPin.CREATE_WALLET,
            OriginScreenToPin.BACKUP_PHRASE,
            OriginScreenToPin.LAUNCHER_SCREEN,
            OriginScreenToPin.LOADER_SCREEN,
            OriginScreenToPin.LOGIN_SCREEN,
            OriginScreenToPin.RESET_PASSWORD_SCREEN,
            OriginScreenToPin.PIN_SCREEN,
            OriginScreenToPin.MANUAL_PAIRING_SCREEN,
            OriginScreenToPin.LOGIN_AUTH_SCREEN,
            OriginScreenToPin.PASSWORD_REQUIRED_SCREEN -> binding.pinLogout.visible()
        }
    }

    private fun init() {
        loadComposableData()

        showDebugEnv()
        setVersionNameAndCode()
        setupCommitHashView()
        setApiOutageMessage()

        with(model) {
            process(PinIntent.CheckIntercomStatus)
            process(PinIntent.CheckNumPinAttempts)
            process(PinIntent.GetAction)
            process(PinIntent.CheckApiStatus)
            process(PinIntent.GetCurrentPin)
            process(PinIntent.CheckFingerprint)
        }
    }

    private fun checkFingerprintStatus() {
        if (lastState.biometricStatus.shouldShowFingerprint && !isChangingPin) {
            showFingerprintDialog()
            model.process(PinIntent.DialogShown)
        }
    }

    private fun handlePasswordErrors(error: PasswordError) {
        when (error) {
            PasswordError.SERVER_CONNECTION_EXCEPTION,
            PasswordError.SERVER_TIMEOUT -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.server_unreachable_exit),
                    type = SnackbarType.Error
                ).show()
                util.restartApp()
            }

            PasswordError.HD_WALLET_EXCEPTION -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.unexpected_error),
                    type = SnackbarType.Error
                ).show()
            }

            PasswordError.ACCOUNT_LOCKED -> showAccountLockedDialog()
            PasswordError.UNKNOWN -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.invalid_password),
                    type = SnackbarType.Error
                ).show()
                showValidationDialog()
            }

            PasswordError.NONE -> {
            }
        }
    }

    private fun handlePayloadErrors(error: PayloadError) {
        when (error) {
            PayloadError.CREDENTIALS_INVALID,
            PayloadError.DECRYPTION_EXCEPTION -> showValidationDialog()

            PayloadError.SERVER_CONNECTION_EXCEPTION,
            PayloadError.SERVER_TIMEOUT -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.server_unreachable_exit),
                    type = SnackbarType.Error
                ).show()
                util.restartApp()
            }

            PayloadError.UNSUPPORTED_VERSION_EXCEPTION -> showWalletVersionNotSupportedDialog("")
            PayloadError.HD_WALLET_EXCEPTION -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.unexpected_error),
                    type = SnackbarType.Error
                ).show()
            }

            PayloadError.INVALID_CIPHER_TEXT -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.password_changed_explanation),
                    type = SnackbarType.Error
                ).show()
                util.clearCredentials()
            }

            PayloadError.ACCOUNT_LOCKED -> showAccountLockedDialog()
            PayloadError.UNKNOWN -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.unexpected_error),
                    type = SnackbarType.Error
                ).show()
            }

            PayloadError.NONE -> {}
        }
    }

    private fun showAccountLockedDialog() {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.account_locked_title)
            .setMessage(com.blockchain.stringResources.R.string.account_locked_message)
            .setCancelable(false)
            .setPositiveButton(com.blockchain.stringResources.R.string.exit) { _, _ -> finish() }
            .create()
            .show()
    }

    private fun handleErrors(error: PinError) {
        when (error) {
            PinError.ERROR_CONNECTION ->
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.api_fail),
                    type = SnackbarType.Warning
                ).show()

            PinError.DONT_MATCH -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.pin_mismatch_error_msg),
                    type = SnackbarType.Warning
                ).show()
            }

            PinError.INVALID_CREDENTIALS -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.invalid_pin),
                    type = SnackbarType.Warning
                )
                    .show()
            }

            PinError.ZEROS_PIN -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.zero_pin),
                    type = SnackbarType.Warning
                ).show()
            }

            PinError.PIN_INCOMPLETE -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.pin_num_digits),
                    type = SnackbarType.Warning
                )
                    .show()
            }

            PinError.CHANGE_TO_EXISTING_PIN -> {
                BlockchainSnackbar.make(
                    binding.root,
                    getString(com.blockchain.stringResources.R.string.change_pin_new_matches_current),
                    type = SnackbarType.Warning
                ).show()
            }

            PinError.CREATE_PIN_FAILED -> {
                util.restartApp()
            }

            PinError.NUM_ATTEMPTS_EXCEEDED -> showMaxAttemptsDialog()
            else -> {}
        }
    }

    private fun onWalletUpgradeFailed() {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.upgrade_fail_heading)
            .setMessage(com.blockchain.stringResources.R.string.upgrade_fail_info)
            .setCancelable(false)
            .setPositiveButton(com.blockchain.stringResources.R.string.exit) { _, _ ->
                util.logout(lastState.isIntercomEnabled)
            }
            .setNegativeButton(com.blockchain.stringResources.R.string.logout) { _, _ ->
                util.logout(lastState.isIntercomEnabled)
                util.restartApp()
            }
            .show()
    }

    private fun shakePinBoxes() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.shake)
        pinBoxList.forEach {
            it.startAnimation(animation)
        }
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(arg0: Animation?) {}
            override fun onAnimationRepeat(arg0: Animation?) {}
            override fun onAnimationEnd(arg0: Animation?) {
                clearPin()
            }
        })
    }

    private fun setPinView(pinScreenView: PinScreenView) {
        when (pinScreenView) {
            PinScreenView.LoginWithPin -> {
                binding.titleBox.setText(com.blockchain.stringResources.R.string.pin_entry)
            }

            PinScreenView.CreateNewPin -> {
                binding.titleBox.setText(com.blockchain.stringResources.R.string.pin_title_create)
            }

            PinScreenView.ConfirmNewPin -> {
                binding.titleBox.setText(com.blockchain.stringResources.R.string.pin_title_confirm)
            }
        }
    }

    private fun setApiOutageMessage() {
        val learnMoreMap = mapOf<String, Uri>("learn_more" to Uri.parse(WALLET_STATUS_URL))
        binding.layoutWarning.warningMessage.let {
            it.movementMethod = LinkMovementMethod.getInstance()
            it.text = StringUtils.getStringWithMappedAnnotations(
                this,
                com.blockchain.stringResources.R.string.wallet_issue_message,
                learnMoreMap
            )
        }
    }

    private fun setVersionNameAndCode() {
        binding.textViewVersionCode.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun setupCommitHashView() {
        binding.debugCommitHash.apply {
            visibleIf { BuildConfig.COMMIT_HASH.isNotEmpty() }
            text = BuildConfig.COMMIT_HASH
            copyHashOnLongClick(this@PinActivity)
        }
    }

    private fun showMaxAttemptsDialog() {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.app_name)
            .setMessage(com.blockchain.stringResources.R.string.password_or_wipe)
            .setCancelable(true)
            .setPositiveButton(com.blockchain.stringResources.R.string.use_password) { _, _ -> showValidationDialog() }
            .setNegativeButton(com.blockchain.stringResources.R.string.common_cancel) { di, _ -> di.dismiss() }
            .show()
    }

    private fun showValidationDialog() {
        val password = AppCompatEditText(this)
        password.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        password.setHint(com.blockchain.stringResources.R.string.password)

        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.app_name)
            .setMessage(getString(com.blockchain.stringResources.R.string.password_entry))
            .setView(this.getAlertDialogPaddedView(password))
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                util.restartApp()
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val pwd = password.text.toString()
                if (pwd.isNotEmpty()) {
                    model.process(PinIntent.ValidatePassword(pwd))
                } else {
                    util.restartApp()
                }
            }.show()
    }

    private fun getIntroducedPin(): String = pinUserInput

    private fun clearPin() {
        pinUserInput = ""
        clearPinBoxes()
        setCursorPinBoxAtIndex(0)
        checkFingerprintStatus()
    }

    private fun onAddDigitChangePinBoxesUI() {
        fillPinBoxAtIndex(getIntroducedPin().length - 1)
        setCursorPinBoxAtIndex(getIntroducedPin().length)
    }

    private fun onAddDigitValidation() {
        if (getIntroducedPin().length == PIN_LENGTH) {
            when (lastState.action) {
                PinScreenView.CreateNewPin -> {
                    when {
                        getIntroducedPin() == "0000" -> {
                            model.process(PinIntent.UpdatePinErrorState(PinError.ZEROS_PIN))
                            errorPinBoxes()
                        }

                        isPinCommon(getIntroducedPin()) -> {
                            showCommonPinWarning()
                        }

                        lastState.isNewPinEqualToCurrentPin() -> {
                            model.process(PinIntent.UpdatePinErrorState(PinError.CHANGE_TO_EXISTING_PIN))
                            clearPin()
                            checkFingerprintStatus()
                        }

                        else -> {
                            validateAndConfirmPin()
                        }
                    }
                }

                PinScreenView.ConfirmNewPin,
                PinScreenView.LoginWithPin -> validateAndConfirmPin()
            }
        }
    }

    private fun validateAndConfirmPin() {
        when {
            lastState.action == PinScreenView.LoginWithPin -> {
                correctPinBoxes()
                model.process(
                    PinIntent.ValidatePIN(
                        getIntroducedPin(),
                        isForValidatingPinForResult,
                        isChangingPin
                    )
                )
            }

            lastState.action == PinScreenView.CreateNewPin -> {
                correctPinBoxes()
                tempNewPin = getIntroducedPin()
                clearPin()
                model.process(PinIntent.UpdateAction(PinScreenView.ConfirmNewPin))
            }

            lastState.action == PinScreenView.ConfirmNewPin && getIntroducedPin() == tempNewPin -> {
                correctPinBoxes()
                model.process(PinIntent.CreatePIN(getIntroducedPin()))
            }

            else -> {
                model.process(PinIntent.UpdatePinErrorState(PinError.DONT_MATCH))
                model.process(PinIntent.UpdateAction(PinScreenView.CreateNewPin))
            }
        }
    }

    private fun updateLoading(loading: Boolean) {
        if (loading) {
            with(binding.lottieProgress) {
                resumeAnimation()
            }
        } else {
            binding.lottieProgress.let {
                it.cancelAnimation()
                it.progress = 0f
            }
        }
    }

    private fun handlePasswordValidated() {
        BlockchainSnackbar.make(
            binding.root,
            getString(com.blockchain.stringResources.R.string.pin_4_strikes_password_accepted)
        )
        startActivity(
            newIntent(
                context = this,
                startForResult = false,
                originScreen = OriginScreenToPin.PIN_SCREEN,
                addFlagsToClear = true,
                referralCode = referralCode
            )
        )
    }

    private fun onPadClicked() {
        onAddDigitChangePinBoxesUI()
        onAddDigitValidation()
        sendAnalytics()
    }

    private fun sendAnalytics() {
        if (getIntroducedPin().length == PIN_LENGTH) {
            when (lastState.action) {
                PinScreenView.CreateNewPin -> analytics.logEventOnce(AnalyticsEvents.WalletSignupPINFirst)
                PinScreenView.ConfirmNewPin -> analytics.logEventOnce(AnalyticsEvents.WalletSignupPINSecond)
                PinScreenView.LoginWithPin -> {}
            }
        }
    }

    private fun PinState.isNewPinEqualToCurrentPin(): Boolean = this.pinStatus.currentPin == getIntroducedPin()

    private fun showWalletVersionNotSupportedDialog(walletVersion: String) {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.warning)
            .setMessage(
                String.format(
                    getString(com.blockchain.stringResources.R.string.unsupported_encryption_version),
                    walletVersion
                )
            )
            .setCancelable(false)
            .setPositiveButton(
                com.blockchain.stringResources.R.string.exit
            ) { _, _ ->
                util.logout(lastState.isIntercomEnabled)
            }
            .setNegativeButton(com.blockchain.stringResources.R.string.logout) { _, _ ->
                util.logout(lastState.isIntercomEnabled)
                util.restartApp()
            }
            .show()
    }

    private fun showCommonPinWarning() {
        AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.common_pin_dialog_title)
            .setMessage(com.blockchain.stringResources.R.string.common_pin_dialog_message)
            .setPositiveButton(
                com.blockchain.stringResources.R.string.common_pin_dialog_try_again
            ) { _, _ ->
                clearPin()
                checkFingerprintStatus()
            }
            .setNegativeButton(
                com.blockchain.stringResources.R.string.common_pin_dialog_continue
            ) { _, _ ->
                validateAndConfirmPin()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun isPinCommon(pin: String): Boolean {
        val commonPins = listOf("1234", "1111", "1212", "7777", "1004")
        return commonPins.contains(pin)
    }

    private fun onDeleteClicked() {
        if (lastState.error != PinError.NONE) {
            clearPin()
        } else {
            clearPinBoxAtIndex(getIntroducedPin().length + 1)
        }

        setCursorPinBoxAtIndex(getIntroducedPin().length)
    }

    private fun fillPinBoxAtIndex(index: Int) {
        pinBoxList.getOrNull(index)?.setImageResource(R.drawable.pin_box_square_default_filled)
    }

    private fun clearPinBoxAtIndex(index: Int) {
        pinBoxList.getOrNull(index)?.setImageResource(R.drawable.pin_box_square_default_empty)
    }

    private fun setCursorPinBoxAtIndex(index: Int) {
        pinBoxList.getOrNull(index)?.setImageResource(R.drawable.pin_box_square_current)
    }

    private fun correctPinBoxes() {
        pinBoxList.forEach {
            it.setImageResource(R.drawable.pin_box_square_correct)
        }
    }

    private fun errorPinBoxes() {
        pinBoxList.forEach {
            it.setImageResource(R.drawable.pin_box_square_error)
        }
    }

    private fun clearPinBoxes() {
        pinBoxList.forEach {
            it.setImageResource(R.drawable.pin_box_square_default_empty)
        }
    }

    private fun finishWithResultOk(pin: String) {
        fraudService.endFlow(FraudFlow.LOGIN)
        val bundle = Bundle()
        bundle.putString(KEY_VALIDATED_PIN, pin)
        val intent = Intent()
        intent.putExtras(bundle)
        this.setResult(RESULT_OK, intent)
        this.finish()
    }

    private fun finishWithResultCanceled() {
        fraudService.endFlow(FraudFlow.LOGIN)
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this) {
            when {
                isForValidatingPinForResult -> {
                    finishWithResultCanceled()
                }

                originScreen == OriginScreenToPin.CHANGE_PIN_SECURITY -> {
                    fraudService.endFlow(FraudFlow.LOGIN)
                    finish()
                }

                else -> {
                    fraudService.endFlow(FraudFlow.LOGIN)
                    appUtil.logout(lastState.isIntercomEnabled)
                }
            }
        }
    }

    private fun showDebugEnv() {
        if (environmentConfig.isRunningInDebugMode()) {
            BlockchainSnackbar.make(
                binding.root,
                "Current environment: " + environmentConfig.environment.name
            )
        }
    }

    private fun showMobileNotice(mobileNoticeDialog: MobileNoticeDialog) {
        if (!this.isFinishing) {
            val alertFragment = MobileNoticeDialogFragment.newInstance(mobileNoticeDialog)
            alertFragment.show(supportFragmentManager, alertFragment.tag)
        }
    }

    private fun finishSignupProcess() {
        util.loadAppWithVerifiedPin(
            loaderActivity = LoaderActivity::class.java,
            loginMethod = when (originScreen) {
                OriginScreenToPin.CREATE_WALLET -> LoginMethod.WALLET_CREATION
                OriginScreenToPin.LAUNCHER_SCREEN -> LoginMethod.PIN
                OriginScreenToPin.LOGIN_AUTH_SCREEN -> LoginMethod.CREDENTIALS
                else -> LoginMethod.UNDEFINED
            },
            referralCode = referralCode
        )
    }

    private fun updateFlexibleNatively(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo
    ) {
        val updatedListener = object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager.completeUpdate()
                }
                if (shouldBeUnregistered(installState.installStatus())) {
                    appUpdateManager.unregisterListener(this)
                }
            }
        }
        appUpdateManager.registerListener(updatedListener)
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            REQUEST_CODE_UPDATE
        )
    }

    private fun updateForcedNatively(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo
    ) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.IMMEDIATE,
            this,
            REQUEST_CODE_UPDATE
        )
    }

    private fun handleForcedUpdateFromStore() {
        val alertDialog = AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.app_name)
            .setMessage(com.blockchain.stringResources.R.string.force_upgrade_message)
            .setPositiveButton(com.blockchain.stringResources.R.string.update, null)
            .setNegativeButton(com.blockchain.stringResources.R.string.exit, null)
            .setCancelable(false)
            .create()

        alertDialog.show()
        // Buttons are done this way to avoid dismissing the dialog
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APP_STORE_URI + packageName)))
            } catch (e: ActivityNotFoundException) {
                // Device doesn't have the Play Store installed, direct them to the official
                // store web page anyway
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(APP_STORE_URL + packageName))
                )
            }
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener {
                util.logout(lastState.isIntercomEnabled)
            }
        fraudService.endFlow(FraudFlow.LOGIN)
    }

    private fun shouldBeUnregistered(installStatus: Int): Boolean {
        return installStatus == InstallStatus.CANCELED ||
            installStatus == InstallStatus.DOWNLOADED ||
            installStatus == InstallStatus.INSTALLED ||
            installStatus == InstallStatus.FAILED
    }

    private fun walletUpgradeRequired(passwordTriesRemaining: Int, isFromPinCreation: Boolean) {
        secondPasswordDialog.validate(
            this,
            object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    model.process(PinIntent.UpgradeWallet(null, isFromPinCreation))
                }

                override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                    model.process(PinIntent.UpgradeWallet(validatedSecondPassword, isFromPinCreation))
                }

                override fun onCancelled() {
                    handleIncorrectPassword(passwordTriesRemaining, isFromPinCreation)
                }
            }
        )
    }

    private fun onUpdateFinished(isFromPinCreation: Boolean) =
        when {
            isChangingPin && biometricsController.isBiometricUnlockEnabled -> enrollBiometrics()
            isFromPinCreation && biometricsController.isBiometricAuthEnabled -> askToUseBiometrics()
            isChangingPin -> finish()
            else -> finishSignupProcess()
        }

    private fun askToUseBiometrics() {
        if (!isBiometricsVisible) {
            RequestBiometricsDialog.newInstance().show(supportFragmentManager, RequestBiometricsDialog.TAG)
            isBiometricsVisible = true
        }
    }

    private fun handleIncorrectPassword(triesRemaining: Int, isFromPinCreation: Boolean) {
        if (triesRemaining > 0) {
            walletUpgradeRequired(triesRemaining - 1, isFromPinCreation)
        } else {
            // TODO: Handle can't remember
        }
    }

    private fun showFingerprintDialog() {
        binding.pinKeyboard.apply {
            withBiometrics = true
        }

        if (lastState.biometricStatus.canShowFingerprint) {
            biometricsController.authenticate(
                this,
                BiometricsType.TYPE_LOGIN,
                object : BiometricsCallback<WalletBiometricData> {
                    override fun onAuthSuccess(unencryptedBiometricData: WalletBiometricData) {
                        correctPinBoxes()
                        model.process(PinIntent.SetCanShowFingerprint(false))
                        model.process(
                            PinIntent.ValidatePIN(
                                pin = unencryptedBiometricData.accessPin,
                                isForValidatingPinForResult = isForValidatingPinForResult
                            )
                        )
                    }

                    override fun onAuthFailed(error: BiometricAuthError) {
                        when (error) {
                            is BiometricAuthError.BiometricAuthLockout -> BiometricPromptUtil.showAuthLockoutDialog(
                                this@PinActivity
                            )

                            is BiometricAuthError.BiometricAuthLockoutPermanent -> {
                                hideBiometricsUi()
                                BiometricPromptUtil.showPermanentAuthLockoutDialog(this@PinActivity)
                            }

                            is BiometricAuthError.BiometricKeysInvalidated -> {
                                hideBiometricsUi()
                                BiometricPromptUtil.showInfoInvalidatedKeysDialog(this@PinActivity)
                            }

                            is BiometricAuthError.BiometricAuthOther -> {
                                hideBiometricsUi()
                                BiometricPromptUtil.showBiometricsGenericError(this@PinActivity, error.error)
                            }

                            else -> {
                                // do nothing - this is handled by the Biometric Prompt framework
                            }
                        }
                    }

                    override fun onAuthCancelled() {
                    }
                }
            )
        }
    }

    override fun enrollBiometrics() {
        biometricsController.authenticate(
            this,
            BiometricsType.TYPE_REGISTER,
            object : BiometricsCallback<WalletBiometricData> {
                override fun onAuthSuccess(data: WalletBiometricData) {
                    model.process(PinIntent.CreatePINSucceeded)
                    finishSignupProcess()
                }

                override fun onAuthFailed(error: BiometricAuthError) {
                    when (error) {
                        is BiometricAuthError.BiometricAuthLockout ->
                            BiometricPromptUtil.showAuthLockoutDialog(this@PinActivity)

                        is BiometricAuthError.BiometricAuthLockoutPermanent -> {
                            hideBiometricsUi()
                            BiometricPromptUtil.showPermanentAuthLockoutDialog(this@PinActivity)
                        }

                        is BiometricAuthError.BiometricKeysInvalidated -> {
                            hideBiometricsUi()
                            BiometricPromptUtil.showInfoInvalidatedKeysDialog(this@PinActivity)
                        }

                        is BiometricAuthError.BiometricAuthOther -> {
                            hideBiometricsUi()
                            BiometricPromptUtil.showBiometricsGenericError(this@PinActivity, error.error)
                        }

                        else -> {
                            // do nothing - this is handled by the Biometric Prompt framework
                        }
                    }
                }

                override fun onAuthCancelled() {
                    if (isChangingPin) {
                        model.process(PinIntent.DisableBiometrics)
                        finishSignupProcess()
                    }
                    // do nothing, the sheet is not dismissed when the user starts the flow
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fraudService.endFlows(FraudFlow.LOGIN, FraudFlow.SIGNUP)
    }

    private fun hideBiometricsUi() {
        binding.pinKeyboard.withBiometrics = false
    }

    private fun showCustomerSupportSheet() {
        showBottomSheet(CustomerSupportSheet.newInstance())
    }

    companion object {
        const val ORIGIN_SCREEN = "origin_screen"
        const val KEY_VALIDATING_PIN_FOR_RESULT = "validating_pin"
        const val KEY_VALIDATED_PIN = "validated_pin"
        const val KEY_REFERRAL_CODE = "referral_code"
        private const val PIN_LENGTH = 4
        private const val REQUEST_CODE_UPDATE = 188
        const val KEY_ORIGIN_SETTINGS = "pin_from_settings"
        const val KEY_VALIDATING_PIN_FOR_RESULT_AND_PAYLOAD = "validating_pin_and_payload"
        const val REQUEST_CODE_VALIDATE_PIN = 88

        fun newIntent(
            context: Context,
            startForResult: Boolean,
            originScreen: OriginScreenToPin,
            addFlagsToClear: Boolean,
            referralCode: String? = null
        ) =
            Intent(context, PinActivity::class.java).apply {
                putExtra(KEY_VALIDATING_PIN_FOR_RESULT, startForResult)
                putExtra(KEY_REFERRAL_CODE, referralCode)
                putExtra(ORIGIN_SCREEN, originScreen)
                if (addFlagsToClear) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

        enum class OriginScreenToPin {
            CHANGE_PIN_SECURITY,
            CREATE_WALLET,
            BACKUP_PHRASE,
            LAUNCHER_SCREEN,
            LOADER_SCREEN,
            LOGIN_SCREEN,
            RESET_PASSWORD_SCREEN,
            PIN_SCREEN,
            MANUAL_PAIRING_SCREEN,
            LOGIN_AUTH_SCREEN,
            PASSWORD_REQUIRED_SCREEN
        }
    }
}
