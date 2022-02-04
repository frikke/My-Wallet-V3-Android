package piuk.blockchain.android.ui.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import com.blockchain.annotations.CommonCode
import com.blockchain.componentlib.alert.abstract.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.scopedInject
import java.util.Locale
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityRecoverFundsBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.customviews.BlockchainSnackbar

internal class RecoverFundsActivity : BaseMvpActivity<RecoverFundsView, RecoverFundsPresenter>(), RecoverFundsView {

    override val alwaysDisableScreenshots: Boolean
        get() = true

    private val recoverFundsPresenter: RecoverFundsPresenter by scopedInject()

    private val binding: ActivityRecoverFundsBinding by lazy {
        ActivityRecoverFundsBinding.inflate(layoutInflater)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    private var progressDialog: MaterialProgressDialog? = null
    private val recoveryPhrase: String
        get() = binding.fieldPassphrase.text.toString().toLowerCase(Locale.US).trim()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.recover_funds),
            backAction = { onBackPressed() }
        )
        with(binding) {
            buttonContinue.setOnClickListener { presenter?.onContinueClicked(recoveryPhrase) }
            fieldPassphrase.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_GO) {
                    presenter?.onContinueClicked(recoveryPhrase)
                }
                true
            }
        }
        onViewReady()
    }

    override fun gotoCredentialsActivity(recoveryPhrase: String) {
        val intent = Intent(this, CreateWalletActivity::class.java)
        intent.putExtra(CreateWalletActivity.RECOVERY_PHRASE, recoveryPhrase)
        startActivity(intent)
    }

    override fun startPinEntryActivity() {
        hideKeyboard()
        PinEntryActivity.startAfterWalletCreation(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun showError(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            type = SnackbarType.Error
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    @CommonCode("Move to base")
    override fun showProgressDialog(@StringRes messageId: Int) {
        dismissProgressDialog()

        if (isFinishing) return

        progressDialog = MaterialProgressDialog(this).apply {
            setCancelable(false)
            setMessage(getString(messageId))
            show()
        }
    }

    override fun createPresenter(): RecoverFundsPresenter = recoverFundsPresenter
    override fun getView(): RecoverFundsView = this

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(Intent(ctx, RecoverFundsActivity::class.java))
        }
    }
}
