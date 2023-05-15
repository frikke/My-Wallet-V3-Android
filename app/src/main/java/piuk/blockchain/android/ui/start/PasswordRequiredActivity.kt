package piuk.blockchain.android.ui.start

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.presentation.koin.scopedInject
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding
import piuk.blockchain.android.fraud.domain.service.FraudFlow
import piuk.blockchain.android.fraud.domain.service.FraudService
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.launcher.LauncherActivityV2
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.ui.settings.security.pin.PinActivity

class PasswordRequiredActivity :
    MvpActivity<PasswordRequiredView, PasswordRequiredPresenter>(),
    PasswordRequiredView {
    private val binding: ActivityPasswordRequiredBinding by lazy {
        ActivityPasswordRequiredBinding.inflate(layoutInflater)
    }

    override val presenter: PasswordRequiredPresenter by scopedInject()
    override val view: PasswordRequiredView = this
    private val walletPrefs: WalletStatusPrefs by inject()
    private val fraudService: FraudService by inject()

    private var isTwoFATimerRunning = false
    private val twoFATimer by lazy {
        object : CountDownTimer(TWO_FA_COUNTDOWN, TWO_FA_STEP) {
            override fun onTick(millisUntilFinished: Long) {
                isTwoFATimerRunning = true
            }

            override fun onFinish() {
                isTwoFATimerRunning = false
                walletPrefs.setResendSmsRetries(3)
            }
        }
    }

    private val accountRecoveryResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // logout and go back to email
        presenter.onForgetWalletConfirmed(redirectLandingToLogin = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupBackPress()
        fraudService.trackFlow(FraudFlow.LOGIN)

        with(binding) {
            walletIdentifier.apply {
                labelText = getString(com.blockchain.stringResources.R.string.wallet_id)
                state = TextInputState.Disabled()
            }
            buttonContinue.apply {
                onClick = {
                    presenter.onContinueClicked(binding.fieldPassword.text.toString())
                }
                text = getString(com.blockchain.stringResources.R.string.btn_continue)
            }
            buttonForget.apply {
                onClick = {
                    presenter.onForgetWalletClicked()
                    fraudService.endFlow(FraudFlow.LOGIN)
                }
                text = getString(com.blockchain.stringResources.R.string.wipe_wallet)
            }
            buttonRecover.setOnClickListener { launchRecoveryFlow() }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.loadWalletGuid()
        presenter.checkEmailAuth(binding.fieldPassword.text.toString())
    }

    override fun onPause() {
        super.onPause()
        presenter.cancelPollAuthStatus()
    }

    override fun showSnackbar(@StringRes messageId: Int, type: SnackbarType) {
        BlockchainSnackbar.make(binding.root, getString(messageId), type = type).show()
    }

    override fun showErrorSnackbarWithParameter(@StringRes messageId: Int, message: String) {
        BlockchainSnackbar.make(binding.root, getString(messageId, message), type = SnackbarType.Error).show()
    }

    override fun restartPage() {
        val intent = Intent(this, LauncherActivityV2::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun resetPasswordField() {
        if (!isFinishing) binding.fieldPassword.setText("")
    }

    override fun showWalletGuid(guid: String) {
        binding.walletIdentifier.value = guid
    }

    override fun goToPinPage() {
        startActivity(
            PinActivity.newIntent(
                context = this,
                startForResult = false,
                originScreen = PinActivity.Companion.OriginScreenToPin.PASSWORD_REQUIRED_SCREEN,
                addFlagsToClear = true
            )
        )
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) =
        updateProgressDialog(
            msg = getString(
                com.blockchain.stringResources.R.string.common_spaced_strings,
                getString(com.blockchain.stringResources.R.string.check_email_to_auth_login),
                secondsRemaining.toString()
            ),
            onCancel = {
                presenter.cancelAuthTimer()
                presenter.cancelPollAuthStatus()
            },
            isCancelable = true
        )

    override fun showForgetWalletWarning() {
        showAlert(
            AlertDialog.Builder(this, com.blockchain.componentlib.R.style.AlertDialogStyle)
                .setTitle(com.blockchain.stringResources.R.string.warning)
                .setMessage(com.blockchain.stringResources.R.string.forget_wallet_warning)
                .setPositiveButton(
                    com.blockchain.stringResources.R.string.forget_wallet
                ) { _, _ -> presenter.onForgetWalletConfirmed() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .create()
        )
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(owner = this) {
            presenter.cancelPollAuthStatus()
            finish()
        }
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        authType: Int,
        guid: String,
        password: String
    ) {
        hideKeyboard()

        val dialog = getTwoFactorDialog(
            this,
            authType,
            walletPrefs,
            positiveAction = {
                presenter.submitTwoFactorCode(
                    responseObject,
                    guid,
                    password,
                    it
                )
            },
            resendAction = { limitReached ->
                if (!limitReached) {
                    presenter.requestNew2FaCode(password, guid)
                } else {
                    showSnackbar(
                        com.blockchain.stringResources.R.string.two_factor_retries_exceeded,
                        SnackbarType.Error
                    )
                    if (!isTwoFATimerRunning) {
                        twoFATimer.start()
                    }
                }
            }
        )

        showAlert(dialog)
    }

    override fun onDestroy() {
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }

    private fun launchRecoveryFlow() {
        accountRecoveryResult.launch(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PASSWORD_RECOVERY_URL)))
    }
}
