package piuk.blockchain.android.ui.recover

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import androidx.annotation.StringRes
import com.blockchain.commonarch.presentation.base.addTransactionAnimation
import com.blockchain.commonarch.presentation.mvi.MviActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAccountRecoveryBinding
import piuk.blockchain.android.ui.reset.ResetAccountFragment
import piuk.blockchain.android.ui.reset.password.ResetPasswordFragment
import piuk.blockchain.android.util.StringUtils

class AccountRecoveryActivity :
    MviActivity<AccountRecoveryModel, AccountRecoveryIntents, AccountRecoveryState, ActivityAccountRecoveryBinding>() {

    override val model: AccountRecoveryModel by scopedInject()

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val email: String by lazy {
        intent.getStringExtra(ResetPasswordFragment.EMAIL) ?: ""
    }

    private val userId: String by lazy {
        intent.getStringExtra(ResetPasswordFragment.USER_ID) ?: ""
    }

    private val recoveryToken: String by lazy {
        intent.getStringExtra(ResetPasswordFragment.RECOVERY_TOKEN) ?: ""
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.account_recover_title),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        initControls()
    }

    override fun initBinding(): ActivityAccountRecoveryBinding = ActivityAccountRecoveryBinding.inflate(layoutInflater)

    override fun render(newState: AccountRecoveryState) {
        when (newState.status) {
            AccountRecoveryStatus.INVALID_PHRASE ->
                showSeedPhraseInputError(R.string.invalid_recovery_phrase_1)
            AccountRecoveryStatus.WORD_COUNT_ERROR ->
                showSeedPhraseInputError(R.string.recovery_phrase_word_count_error)
            AccountRecoveryStatus.RECOVERY_SUCCESSFUL -> {
                launchResetPasswordFlow(newState.seedPhrase)
            }
            AccountRecoveryStatus.RECOVERY_FAILED -> {
                analytics.logEvent(AccountRecoveryAnalytics.RecoveryFailed(false))
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.restore_failed),
                    type = SnackbarType.Error
                ).show()
            }
            AccountRecoveryStatus.RESET_KYC_FAILED ->
                BlockchainSnackbar.make(
                    binding.root,
                    getString(R.string.reset_kyc_failed),
                    type = SnackbarType.Error
                ).show()
            else -> {
                // Do nothing.
            }
        }
        binding.progressBar.visibleIf { isRecoveryInProgress(newState) }
    }

    private fun isRecoveryInProgress(newState: AccountRecoveryState) =
        newState.status == AccountRecoveryStatus.VERIFYING_SEED_PHRASE ||
            newState.status == AccountRecoveryStatus.RECOVERING_CREDENTIALS ||
            newState.status == AccountRecoveryStatus.RESETTING_KYC

    private fun initControls() {
        with(binding) {
            recoveryPhaseText.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {
                        binding.recoveryPhaseTextLayout.apply {
                            isErrorEnabled = false
                            error = ""
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
            resetAccountLabel.apply {
                visibleIf { email.isNotEmpty() && userId.isNotEmpty() && recoveryToken.isNotEmpty() }
                text = StringUtils.getStringWithMappedAnnotations(
                    context = this@AccountRecoveryActivity,
                    stringId = R.string.reset_account_notice,
                    linksMap = emptyMap(),
                    onClick = { launchResetAccountFlow() }
                )
                movementMethod = LinkMovementMethod.getInstance()
            }
            resetKycLabel.text = getString(R.string.reset_kyc_notice_1)

            verifyButton.setOnClickListener {
                analytics.logEvent(AccountRecoveryAnalytics.MnemonicEntered(isCustodialAccount = false))

                this@AccountRecoveryActivity.hideKeyboard()
                model.process(
                    AccountRecoveryIntents.VerifySeedPhrase(
                        seedPhrase = recoveryPhaseText.text?.toString() ?: ""
                    )
                )
            }
        }
    }

    private fun launchResetAccountFlow() {
        analytics.logEvent(AccountRecoveryAnalytics.ResetClicked(isCustodialAccount = true))
        hideKeyboard()
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                binding.fragmentContainer.id,
                ResetAccountFragment.newInstance(
                    email = email,
                    userId = userId,
                    recoveryToken = recoveryToken
                ),
                ResetAccountFragment::class.simpleName
            )
            .addToBackStack(ResetAccountFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun launchResetPasswordFlow(recoveryPhrase: String) {
        supportFragmentManager.beginTransaction()
            .addTransactionAnimation()
            .replace(
                binding.fragmentContainer.id,
                ResetPasswordFragment.newInstance(
                    shouldResetKyc = false,
                    email = email,
                    recoveryPhrase = recoveryPhrase
                ),
                ResetPasswordFragment::class.simpleName
            )
            .addToBackStack(ResetPasswordFragment::class.simpleName)
            .commitAllowingStateLoss()
    }

    private fun showSeedPhraseInputError(@StringRes errorText: Int) {
        binding.recoveryPhaseTextLayout.apply {
            isErrorEnabled = true
            error = getString(errorText)
        }
    }
}
