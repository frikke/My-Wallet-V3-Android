package piuk.blockchain.android.ui.addresses

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.blockchain.analytics.events.WalletAnalytics
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.bch.BchCryptoWalletAccount
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.componentlib.alert.SnackbarType
import com.blockchain.componentlib.legacy.MaterialProgressDialog
import com.blockchain.componentlib.utils.QRCodeEncoder
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.presentation.extensions.getAccount
import com.blockchain.presentation.extensions.putAccount
import com.blockchain.presentation.koin.scopedInject
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.WriterException
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.databinding.DialogAccountEditBinding
import timber.log.Timber

class AccountEditSheet : SlidingModalBottomDialog<DialogAccountEditBinding>() {

    interface Host : SlidingModalBottomDialog.Host {
        fun onStartTransferFunds(account: CryptoNonCustodialAccount)
    }

    override val host: Host by lazy {
        super.host as? Host
            ?: throw IllegalStateException("Host fragment is not a AccountEditSheet.Host")
    }

    val account: CryptoAccount?
        get() = arguments?.getAccount(PARAM_ACCOUNT) as? CryptoAccount

    override fun initControls(binding: DialogAccountEditBinding) {
        (account as? CryptoNonCustodialAccount)?.let {
            configureUi(it)
        } ?: dismiss()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogAccountEditBinding =
        DialogAccountEditBinding.inflate(inflater, container, false)

    private fun configureUi(account: CryptoNonCustodialAccount) {
        configureTransfer(account)
        configureAccountLabel(account)
        configureMakeDefault(account)
        configureShowXpub(account)
        configureArchive(account, account.isArchived)
    }

    @SuppressLint("CheckResult")
    private fun configureTransfer(account: CryptoNonCustodialAccount) {
        with(binding.transferContainer) {
            gone()
            isClickable = false
            setOnClickListener { }

            if (!account.isInternalAccount) {
                account.balanceRx().firstOrError().map { it.withdrawable }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = {
                            visible()
                            if (account.isArchived) {
                                alpha = DISABLED_ALPHA
                            } else {
                                alpha = ENABLED_ALPHA
                                isClickable = true
                                setOnClickListener { handleTransfer(account) }
                            }
                        },
                        onError = {
                            Timber.e("Failed getting balance for imported account: $it")
                        }
                    )
            }
        }
    }

    private fun handleTransfer(account: CryptoNonCustodialAccount) {
        dismiss()
        host.onStartTransferFunds(account)
    }

    private fun configureAccountLabel(account: CryptoNonCustodialAccount) {
        with(binding.accountName) {
            text = account.label
            if (account.isArchived) {
                alpha =
                    DISABLED_ALPHA
                isClickable = false
                setOnClickListener { }
            } else {
                alpha = ENABLED_ALPHA
                isClickable = true
                setOnClickListener {
                    promptForAccountLabel(
                        ctx = requireContext(),
                        title = com.blockchain.stringResources.R.string.edit_wallet_name,
                        msg = com.blockchain.stringResources.R.string.edit_wallet_name_helper_text,
                        initialText = account.label,
                        okAction = { s -> handleUpdateLabel(s, account) }
                    )
                }
            }
        }
    }

    private fun showError(@StringRes msgId: Int) =
        BlockchainSnackbar.make(
            dialog?.window?.decorView ?: binding.root,
            getString(msgId),
            duration = Snackbar.LENGTH_SHORT,
            type = SnackbarType.Error
        ).show()

    // This should all be in a model or presenter. Move it all once the updates are working
    private val disposables = CompositeDisposable()
    private val coincore: Coincore by scopedInject()

    private fun handleUpdateLabel(newLabel: String, account: CryptoNonCustodialAccount) {
        val labelCopy = newLabel.trim { it.isWhitespace() }
        if (labelCopy.isEmpty()) {
            showError(com.blockchain.stringResources.R.string.label_cant_be_empty)
        } else {
            disposables += coincore.isLabelUnique(newLabel)
                .flatMapCompletable {
                    if (it) {
                        account.updateLabel(labelCopy)
                    } else {
                        showError(com.blockchain.stringResources.R.string.label_name_match)
                        Completable.complete()
                    }
                }.observeOn(AndroidSchedulers.mainThread())
                .showProgress()
                .updateUi(account)
                .subscribeBy(
                    onComplete = {
                        binding.accountName.text = newLabel
                        analytics.logEvent(WalletAnalytics.EditWalletName)
                    },
                    onError = {
                        showError(com.blockchain.stringResources.R.string.remote_save_failed)
                    }
                )
        }
    }

    private fun configureMakeDefault(account: CryptoNonCustodialAccount) {
        with(binding.defaultContainer) {
            when {
                account.isDefault -> gone()
                account.isArchived -> {
                    visible()
                    alpha =
                        DISABLED_ALPHA
                    isClickable = false
                }

                account.isInternalAccount -> {
                    visible()
                    alpha =
                        ENABLED_ALPHA
                    isClickable = true
                    setOnClickListener { makeDefault(account) }
                }

                else -> gone()
            }
        }
    }

    private fun makeDefault(account: CryptoNonCustodialAccount) {
        disposables += account.setAsDefault()
            .observeOn(AndroidSchedulers.mainThread())
            .showProgress()
            .updateUi(account)
            .subscribeBy(
                onComplete = {
                    analytics.logEvent(WalletAnalytics.ChangeDefault)
                    //                    updateReceiveAddressShortcuts()
                },
                onError = {
                    showError(com.blockchain.stringResources.R.string.remote_save_failed)
                }
            )
    }

    private fun configureShowXpub(account: CryptoNonCustodialAccount) {
        with(binding) {
            if (account.isInternalAccount) {
                tvXpub.setText(com.blockchain.stringResources.R.string.extended_public_key)
                tvXpubDescription.visible()

                if (account.currency == CryptoCurrency.BCH) {
                    tvXpubDescription.setText(
                        com.blockchain.stringResources.R.string.extended_public_key_description_bch_only
                    )
                } else {
                    tvXpubDescription.setText(com.blockchain.stringResources.R.string.extended_public_key_description)
                }
            } else {
                tvXpub.setText(com.blockchain.stringResources.R.string.address)
                tvXpubDescription.gone()
            }

            if (account.isArchived) {
                xpubContainer.alpha =
                    DISABLED_ALPHA
                xpubContainer.isClickable = false
                xpubContainer.setOnClickListener { }
            } else {
                xpubContainer.alpha = ENABLED_ALPHA
                xpubContainer.isClickable = true
                xpubContainer.setOnClickListener { handleShowXpub(account) }
            }
        }
    }

    private fun handleShowXpub(account: CryptoNonCustodialAccount) =
        if (account.isInternalAccount) {
            promptXpubShareWarning(requireContext()) { showXpubAddress(account) }
        } else {
            showAccountAddress(account)
        }

    private fun showXpubAddress(account: CryptoNonCustodialAccount) {
        require(account.isInternalAccount)

        val qrString: String = account.xpubAddress
        generateQrCode(qrString)?.let { bmp ->
            context?.let { ctx ->
                showAddressQrCode(
                    ctx,
                    com.blockchain.stringResources.R.string.extended_public_key,
                    com.blockchain.stringResources.R.string.scan_this_code,
                    com.blockchain.stringResources.R.string.copy_xpub,
                    bmp,
                    qrString
                )
            }
            analytics.logEvent(WalletAnalytics.ShowXpub)
        }
    }

    private fun showAccountAddress(account: CryptoNonCustodialAccount) {
        require(!account.isInternalAccount)

        val qrString: String = account.xpubAddress
        generateQrCode(qrString)?.let {
            showAddressQrCode(
                requireContext(),
                com.blockchain.stringResources.R.string.address,
                qrString,
                com.blockchain.stringResources.R.string.copy_address,
                it,
                qrString
            )
            analytics.logEvent(WalletAnalytics.ShowXpub)
        }
    }

    private fun generateQrCode(qrString: String) =
        try {
            QRCodeEncoder.encodeAsBitmap(
                qrString,
                QR_CODE_DIMENSION
            )
        } catch (e: WriterException) {
            Timber.e(e)
            null
        }

    private fun configureArchive(account: CryptoNonCustodialAccount, isArchived: Boolean) {
        if (isArchived) {
            binding.tvArchiveHeader.setText(com.blockchain.stringResources.R.string.unarchive)
            binding.tvArchiveDescription.setText(com.blockchain.stringResources.R.string.archived_description)
            with(binding.archiveContainer) {
                alpha = ENABLED_ALPHA
                visibility = View.VISIBLE
                isClickable = true
                setOnClickListener { toggleArchived(account, isArchived) }
            }
        } else {
            if (account.isDefault) {
                binding.tvArchiveHeader.setText(com.blockchain.stringResources.R.string.archive)
                binding.tvArchiveDescription.setText(
                    com.blockchain.stringResources.R.string.default_account_description
                )
                with(binding.archiveContainer) {
                    alpha =
                        DISABLED_ALPHA
                    visibility = View.VISIBLE
                    isClickable = false
                    setOnClickListener { /* not clickable */ }
                }
            } else {
                binding.tvArchiveHeader.setText(com.blockchain.stringResources.R.string.archive)
                binding.tvArchiveDescription.setText(com.blockchain.stringResources.R.string.not_archived_description)
                with(binding.archiveContainer) {
                    alpha =
                        ENABLED_ALPHA
                    visibility = View.VISIBLE
                    isClickable = true
                    setOnClickListener { toggleArchived(account, isArchived) }
                }
            }
        }
    }

    private fun toggleArchived(account: CryptoNonCustodialAccount, isArchived: Boolean) {
        val title =
            if (isArchived) com.blockchain.stringResources.R.string.unarchive else
                com.blockchain.stringResources.R.string.archive
        val msg =
            if (isArchived) com.blockchain.stringResources.R.string.unarchive_are_you_sure else
                com.blockchain.stringResources.R.string.archive_are_you_sure
        promptArchive(
            requireContext(),
            title,
            msg
        ) { handleToggleArchive(account, isArchived) }
    }

    private fun handleToggleArchive(account: CryptoNonCustodialAccount, isArchived: Boolean) {
        disposables += if (isArchived) {
            account.unarchive()
        } else {
            account.archive()
        }.observeOn(AndroidSchedulers.mainThread())
            .showProgress()
            .updateUi(account)
            .subscribeBy(
                onError = { showError(com.blockchain.stringResources.R.string.remote_save_failed) },
                onComplete = {
                    configureArchive(account, !isArchived)
                    if (!account.isArchived) {
                        analytics.logEvent(WalletAnalytics.UnArchiveWallet)
                    } else
                        analytics.logEvent(WalletAnalytics.ArchiveWallet)
                }
            )
    }

    companion object {
        private const val DISABLED_ALPHA = 0.5f
        private const val ENABLED_ALPHA = 1.0f

        private const val QR_CODE_DIMENSION = 260

        private const val PARAM_ACCOUNT = "PARAM_ACCOUNT"

        fun newInstance(account: SingleAccount): AccountEditSheet =
            AccountEditSheet().apply {
                arguments = Bundle().apply {
                    putAccount(PARAM_ACCOUNT, account)
                }
            }
    }

    private var progressDialog: MaterialProgressDialog? = null

    @UiThread
    private fun doShowProgress() {
        doHideProgress()
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setCancelable(false)
            setMessage(com.blockchain.stringResources.R.string.please_wait)
            show()
        }
    }

    @UiThread
    private fun doHideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun Completable.showProgress() =
        this.doOnSubscribe { doShowProgress() }
            .doOnTerminate { doHideProgress() }

    private fun Completable.updateUi(account: CryptoNonCustodialAccount) =
        this.doOnComplete { configureUi(account) }
}

private val CryptoNonCustodialAccount.isInternalAccount: Boolean
    get() = (this as? BtcCryptoWalletAccount)?.isHDAccount
        ?: (this as? BchCryptoWalletAccount)?.let { true }
        ?: throw java.lang.IllegalStateException("Unexpected asset type")
