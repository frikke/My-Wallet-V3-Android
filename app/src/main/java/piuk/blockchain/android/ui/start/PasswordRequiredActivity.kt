package piuk.blockchain.android.ui.start

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.redesignPart2FeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.WalletStatus
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity

class PasswordRequiredActivity :
    MvpActivity<PasswordRequiredView, PasswordRequiredPresenter>(),
    PasswordRequiredView {
    private val binding: ActivityPasswordRequiredBinding by lazy {
        ActivityPasswordRequiredBinding.inflate(layoutInflater)
    }

    override val presenter: PasswordRequiredPresenter by scopedInject()
    override val view: PasswordRequiredView = this
    private val walletPrefs: WalletStatus by inject()

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

    private val redesign: FeatureFlag by inject(redesignPart2FeatureFlag)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {

            walletIdentifier.apply {
                labelText = getString(R.string.wallet_id)
                state = TextInputState.Disabled()
            }

            buttonContinue.apply {
                onClick = {
                    presenter.onContinueClicked(binding.fieldPassword.text.toString())
                }
                text = getString(R.string.btn_continue)
            }
            buttonForget.apply {
                onClick = {
                    presenter.onForgetWalletClicked()
                }
                text = getString(R.string.wipe_wallet)
            }
            buttonRecover.setOnClickListener { launchRecoveryFlow() }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.loadWalletGuid()
    }

    override fun showSnackbar(@StringRes messageId: Int, type: SnackbarType) {
        BlockchainSnackbar.make(binding.root, getString(messageId), type = type).show()
    }

    override fun showErrorSnackbarWithParameter(@StringRes messageId: Int, message: String) {
        BlockchainSnackbar.make(binding.root, getString(messageId, message), type = SnackbarType.Error).show()
    }

    override fun restartPage() {
        val intent = Intent(this, LauncherActivity::class.java)
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
        // TODO remove ff
        redesign.enabled.onErrorReturnItem(false).subscribeBy(
            onSuccess = { isEnabled ->
                if (isEnabled) {
                    startActivity(PinActivity.newIntent(this))
                } else {
                    startActivity(Intent(this, PinEntryActivity::class.java))
                }
            }
        )
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) =
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)

    override fun showForgetWalletWarning() {
        showAlert(
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.forget_wallet_warning)
                .setPositiveButton(R.string.forget_wallet) { _, _ -> presenter.onForgetWalletConfirmed() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .create()
        )
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {
        hideKeyboard()

        val dialog = getTwoFactorDialog(
            this, authType,
            walletPrefs,
            positiveAction = {
                presenter.submitTwoFactorCode(
                    responseObject,
                    sessionId,
                    guid,
                    password,
                    it
                )
            }, resendAction = { limitReached ->
            if (!limitReached) {
                presenter.requestNew2FaCode(password, guid)
            } else {
                showSnackbar(R.string.two_factor_retries_exceeded, SnackbarType.Error)
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

    private fun launchRecoveryFlow() = RecoverFundsActivity.start(this)
}
