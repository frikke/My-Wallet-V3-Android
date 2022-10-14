package piuk.blockchain.android.ui.addresses

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.databinding.ToolbarGeneralBinding
import com.blockchain.presentation.koin.scopedInject
import com.blockchain.utils.consume
import com.blockchain.utils.unsafeLazy
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityAddressesBinding
import piuk.blockchain.android.ui.addresses.adapter.AccountAdapter
import piuk.blockchain.android.ui.addresses.adapter.AccountListItem
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.SecondPasswordDialog
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class AddressesActivity :
    MvpActivity<AccountView, AccountPresenter>(),
    AccountView,
    AccountAdapter.Listener,
    AccountEditSheet.Host {

    private val secondPasswordDialog: SecondPasswordDialog by scopedInject()
    private val compositeDisposable = CompositeDisposable()

    private val binding: ActivityAddressesBinding by lazy {
        ActivityAddressesBinding.inflate(layoutInflater)
    }

    private val accountsAdapter: AccountAdapter by unsafeLazy {
        AccountAdapter(this)
    }

    private lateinit var onBackPressCloseHeaderCallback: OnBackPressedCallback

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupBackPress()

        updateToolbar(
            toolbarTitle = getString(R.string.drawer_addresses),
            backAction = { onBackPressedDispatcher.onBackPressed() }
        )
        onBackPressCloseHeaderCallback.isEnabled = binding.currencyHeader.isOpen()
        with(binding.currencyHeader) {
            setCurrentlySelectedCurrency(CryptoCurrency.BTC)
            setSelectionListener { presenter.refresh(it) }
            setAnimationListener { isOpen ->
                // enable the callback only when the header is open so it can be closed on back press
                // otherwise disable it so system can handle back press
                onBackPressCloseHeaderCallback.isEnabled = isOpen
            }
        }

        with(binding.recyclerviewAccounts) {
            layoutManager = LinearLayoutManager(this@AddressesActivity)
            itemAnimator = null
            setHasFixedSize(true)
            adapter = accountsAdapter
            addItemDecoration(
                BlockchainListDividerDecor(context)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> consume { onBackPressedDispatcher.onBackPressed() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupBackPress() {
        onBackPressCloseHeaderCallback = onBackPressedDispatcher.addCallback(owner = this) {
            binding.currencyHeader.close()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            with(binding.currencyHeader) {
                if (isTouchOutside(event) && isOpen()) {
                    close()
                    return false
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onCreateNewClicked() {
        Timber.d("Click new account")
        createNewAccount()
    }

    override fun onAccountClicked(account: CryptoAccount) {
        Timber.d("Click ${account.label}")
        showBottomSheet(AccountEditSheet.newInstance(account))
    }

    override fun onImportAddressClicked() {
        Timber.d("Click import account")
        compositeDisposable += secondPasswordDialog.secondPassword(this)
            .subscribeBy(
                onSuccess = { showScanActivity() },
                onComplete = { showScanActivity() },
                onError = { }
            )
    }

    private fun showScanActivity() {
        QrScanActivity.start(this, QrExpected.IMPORT_KEYS_QR)
    }

    private fun createNewAccount() {
        compositeDisposable += secondPasswordDialog.secondPassword(this)
            .subscribeBy(
                onSuccess = { password ->
                    promptForAccountLabel(
                        ctx = this@AddressesActivity,
                        title = R.string.create_a_new_wallet,
                        msg = R.string.create_a_new_wallet_helper_text,
                        okAction = { presenter.createNewAccount(it, password) }
                    )
                },
                onComplete = {
                    promptForAccountLabel(
                        ctx = this@AddressesActivity,
                        title = R.string.create_a_new_wallet,
                        msg = R.string.create_a_new_wallet_helper_text,
                        okAction = { presenter.createNewAccount(it) }
                    )
                },
                onError = { }
            )
    }

    override fun renderAccountList(
        asset: AssetInfo,
        internal: List<CryptoNonCustodialAccount>,
        imported: List<CryptoNonCustodialAccount>
    ) {
        accountsAdapter.items = mutableListOf<AccountListItem>().apply {
            if (internal.isNotEmpty()) {
                add(AccountListItem.InternalHeader(enableCreate = asset == CryptoCurrency.BTC))
                addAll(internal.map { AccountListItem.Account(it) })
            }

            add(AccountListItem.ImportedHeader(enableImport = asset == CryptoCurrency.BTC))
            if (imported.isNotEmpty()) {
                addAll(imported.map { AccountListItem.Account(it) })
            }
        }.toList()
    }

    override fun onResume() {
        super.onResume()
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode == RESULT_OK && requestCode == QrScanActivity.SCAN_URI_RESULT &&
                data.getRawScanData() != null -> {
                data.getRawScanData()?.let {
                    handleImportScan(it)
                } ?: showError(R.string.privkey_error)
            }
            requestCode == TX_FLOW_REQUEST -> presenter.refresh(binding.currencyHeader.getSelectedCurrency())
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun handleImportScan(scanData: String) {
        val walletPassword = secondPasswordDialog.verifiedPassword.takeIf { it?.isNotEmpty() == true }
        if (presenter.importRequiresPassword(scanData)) {
            promptImportKeyPassword(this) { password ->
                presenter.importScannedAddress(scanData, password, walletPassword)
            }
        } else {
            presenter.importScannedAddress(scanData, walletPassword)
        }
    }

    override fun onStartTransferFunds(account: CryptoNonCustodialAccount) {
        launchFlow(account)
    }

    override fun onSheetClosed() {
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun showRenameImportedAddressDialog(account: CryptoNonCustodialAccount) =
        promptForAccountLabel(
            ctx = this,
            title = R.string.app_name,
            msg = R.string.label_address,
            initialText = account.label,
            okAction = { presenter.updateImportedAddressLabel(it, account) },
            okBtnText = R.string.save_name,
            cancelText = R.string.polite_no
        )

    override fun showError(@StringRes message: Int) =
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()

    override fun showSuccess(@StringRes message: Int) {
        BlockchainSnackbar.make(
            binding.root,
            getString(message),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Success
        ).show()
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun showTransferFunds(account: CryptoNonCustodialAccount) {
        promptTransferFunds(this) { launchFlow(account) }
    }

    private fun launchFlow(sourceAccount: CryptoAccount) {
        startActivityForResult(
            TransactionFlowActivity.newIntent(
                context = this,
                sourceAccount = sourceAccount,
                action = AssetAction.Send
            ),
            TX_FLOW_REQUEST
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    override val presenter: AccountPresenter by scopedInject()
    override val view: AccountView
        get() = this

    companion object {
        private const val TX_FLOW_REQUEST = 567

        fun newIntent(context: Context): Intent =
            Intent(context, AddressesActivity::class.java)
    }
}
