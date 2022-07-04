package piuk.blockchain.android.ui.start

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.WalletStatusPrefs
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityManualPairingBinding
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.ui.settings.v2.security.pin.PinActivity
import piuk.blockchain.android.util.AfterTextChangedWatcher

class ManualPairingActivity : MvpActivity<ManualPairingView, ManualPairingPresenter>(), ManualPairingView {

    private val binding: ActivityManualPairingBinding by lazy {
        ActivityManualPairingBinding.inflate(layoutInflater)
    }

    private val prefilledGuid: String by lazy {
        intent.getStringExtra(PREFILLED_GUID) ?: ""
    }

    override val view: ManualPairingView = this
    override val presenter: ManualPairingPresenter by scopedInject()
    private val walletPrefs: WalletStatusPrefs by inject()

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

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

    private val guid: String
        get() = binding.walletId.text.toString()
    private val password: String
        get() = binding.walletPass.text.toString()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.manual_pairing),
            backAction = { onBackPressed() }
        )
        with(binding) {
            binding.walletId.disableInputForDemoAccount()
            commandNext.setOnClickListener { presenter.onContinueClicked(guid, password) }
            binding.walletId.setText(prefilledGuid)
            walletPass.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_GO) {
                    presenter.onContinueClicked(guid, password)
                }
                true
            }
        }
    }

    override fun showSnackbar(@StringRes messageId: Int, type: SnackbarType) {
        BlockchainSnackbar.make(binding.root, getString(messageId), type = type).show()
    }

    override fun showErrorSnackbarWithParameter(@StringRes messageId: Int, message: String) {
        BlockchainSnackbar.make(binding.root, getString(messageId, message), type = SnackbarType.Error).show()
    }

    override fun goToPinPage() {
        startActivity(
            PinActivity.newIntent(
                context = this,
                startForResult = false,
                originScreen = PinActivity.Companion.OriginScreenToPin.MANUAL_PAIRING_SCREEN,
                addFlagsToClear = true
            )
        )
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) =
        updateProgressDialog(
            msg = getString(
                R.string.common_spaced_strings,
                getString(R.string.check_email_to_auth_login),
                secondsRemaining.toString()
            ),
            onCancel = {
                presenter.cancelAuthTimer()
                presenter.cancelPollAuthStatus()
            },
            isCancelable = true
        )

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {

        hideKeyboard()

        val dialog = getTwoFactorDialog(this, authType, walletPrefs, positiveAction = {
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
        })

        showAlert(dialog)
    }

    override fun resetPasswordField() {
        if (!isFinishing)
            binding.walletPass.setText("")
    }

    public override fun onDestroy() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        presenter.cancelPollAuthStatus()
    }

    private fun TextInputEditText.disableInputForDemoAccount() {
        addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(text: Editable) {
                if (text.toString().isNotEmpty() && text.toString() == BuildConfig.PLAY_STORE_DEMO_WALLET_ID)
                    inputType = InputType.TYPE_NULL
            }
        })
    }

    companion object {
        private const val PREFILLED_GUID = "PREFILLED_GUID"
        fun newInstance(activity: Activity, guid: String?): Intent {
            val intent = Intent(activity, ManualPairingActivity::class.java)
            intent.putExtra(PREFILLED_GUID, guid)
            return intent
        }
    }
}
