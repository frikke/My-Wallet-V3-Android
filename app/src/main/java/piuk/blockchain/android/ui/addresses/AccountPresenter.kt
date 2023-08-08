package piuk.blockchain.android.ui.addresses

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.AddressAnalytics
import com.blockchain.analytics.events.AnalyticsEvents
import com.blockchain.analytics.events.WalletAnalytics
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.MultipleWalletsAsset
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.bch.BchCryptoWalletAccount
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import timber.log.Timber

interface AccountView : MvpView {
    fun showError(@StringRes message: Int)
    fun showSuccess(@StringRes message: Int)
    fun showRenameImportedAddressDialog(account: CryptoNonCustodialAccount)
    fun renderAccountList(
        asset: AssetInfo,
        internal: List<CryptoNonCustodialAccount>,
        imported: List<CryptoNonCustodialAccount>
    )

    fun showTransferFunds(account: CryptoNonCustodialAccount)
}

class AccountPresenter internal constructor(
    private val privateKeyFactory: PrivateKeyFactory,
    private val coincore: Coincore,
    private val analytics: Analytics
) : MvpPresenter<AccountView>() {

    override fun onViewCreated() {}

    override fun onViewAttached() {
        analytics.logEvent(AnalyticsEvents.AccountsAndAddresses)
    }

    override fun onViewDetached() {}

    fun refresh(asset: AssetInfo) {
        fetchAccountList(coincore[asset])
    }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true

    private class NameInUseException : Exception()

    /**
     * Derive new Account from seed
     *
     * @param accountLabel A label for the account to be created
     */
    @SuppressLint("CheckResult")
    internal fun createNewAccount(accountLabel: String, secondPassword: String? = null) {
        val btcAsset = coincore[CryptoCurrency.BTC] as MultipleWalletsAsset
        val bchAsset = coincore[CryptoCurrency.BCH] as MultipleWalletsAsset

        compositeDisposable += coincore.isLabelUnique(accountLabel)
            .flatMap { isUnique ->
                if (!isUnique) {
                    Single.error(NameInUseException())
                } else {
                    btcAsset.createWalletFromLabel(accountLabel, secondPassword)
                }
            }.map { it as BtcCryptoWalletAccount }.flatMapCompletable {
                bchAsset.createWalletFromAddress(it.xpubAddress)
            }.observeOn(AndroidSchedulers.mainThread())
            .showProgress()
            .subscribeBy(
                onComplete = {
                    view?.showSuccess(com.blockchain.stringResources.R.string.remote_save_ok)
                    analytics.logEvent(WalletAnalytics.AddNewWallet)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    when (throwable) {
                        is DecryptionException -> view?.showError(
                            com.blockchain.stringResources.R.string.double_encryption_password_error
                        )
                        is NameInUseException -> view?.showError(
                            com.blockchain.stringResources.R.string.label_name_match
                        )
                        else -> view?.showError(com.blockchain.stringResources.R.string.unexpected_error)
                    }
                }
            )
    }

    internal fun updateImportedAddressLabel(newLabel: String, account: CryptoNonCustodialAccount) {
        compositeDisposable += account.updateLabel(newLabel)
            .observeOn(AndroidSchedulers.mainThread())
            .showProgress()
            .subscribeBy(
                onComplete = {
                    view?.showSuccess(com.blockchain.stringResources.R.string.remote_save_ok)
                    checkBalanceForTransfer(account)
                },
                onError = {
                    view?.showError(com.blockchain.stringResources.R.string.remote_save_failed)
                }
            )
    }

    internal fun checkBalanceForTransfer(account: CryptoNonCustodialAccount) {
        compositeDisposable += account.balanceRx().firstOrError().map { it.withdrawable }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                if (it.isPositive) {
                    view?.showTransferFunds(account)
                }
            }
    }

    internal fun importRequiresPassword(data: String): Boolean =
        privateKeyFactory.getFormat(data) == PrivateKeyFactory.BIP38

    internal fun importScannedAddress(
        keyData: String,
        walletSecondPassword: String?
    ) {
        val format = privateKeyFactory.getFormat(keyData)
        if (checkCanImport(keyData, format)) {
            require(format != PrivateKeyFactory.BIP38)
            importAddress(keyData, format, null, walletSecondPassword)
        }
    }

    internal fun importScannedAddress(
        keyData: String,
        keyPassword: String,
        walletSecondPassword: String?
    ) {
        val format = privateKeyFactory.getFormat(keyData)
        if (checkCanImport(keyData, format)) {
            require(format == PrivateKeyFactory.BIP38)
            importAddress(keyData, format, keyPassword, walletSecondPassword)
        }
    }

    private fun checkCanImport(keyData: String, format: String?) =
        if (format == null) {
            val btcAsset = coincore[CryptoCurrency.BTC]
            view?.showError(
                if (btcAsset.isValidAddress(keyData)) {
                    com.blockchain.stringResources.R.string.watch_only_not_supported
                } else
                    com.blockchain.stringResources.R.string.privkey_error
            )
            false
        } else {
            true
        }

    private fun importAddress(
        keyData: String,
        keyFormat: String,
        keyPassword: String?,
        walletSecondPassword: String?
    ) {
        val btcAsset = coincore[CryptoCurrency.BTC] as MultipleWalletsAsset
        compositeDisposable += btcAsset.importWalletFromKey(
            keyData,
            keyFormat,
            keyPassword,
            walletSecondPassword
        ).map {
            it as CryptoNonCustodialAccount
        }.showProgress()
            .subscribeBy(
                onSuccess = {
                    view?.showSuccess(com.blockchain.stringResources.R.string.private_key_successfully_imported)
                    view?.showRenameImportedAddressDialog(it)
                    analytics.logEvent(AddressAnalytics.ImportBTCAddress)
                },
                onError = {
                    view?.showError(com.blockchain.stringResources.R.string.no_private_key)
                }
            )
    }

    private fun fetchAccountList(asset: Asset) {
        require(asset is MultipleWalletsAsset)

        compositeDisposable += asset.accountGroup(AssetFilter.NonCustodial)
            .map {
                it.accounts
            }.subscribeBy(
                onSuccess = { processCoincoreList(asset.currency, it) },
                onError = { e ->
                    Timber.e("Failed to get account list for asset: $e")
                }
            )
    }

    private fun processCoincoreList(asset: AssetInfo, list: SingleAccountList) {
        val internal = mutableListOf<CryptoNonCustodialAccount>()
        val imported = mutableListOf<CryptoNonCustodialAccount>()

        list.filterIsInstance<CryptoNonCustodialAccount>()
            .forEach {
                if (it.isInternalAccount) {
                    internal.add(it)
                } else {
                    imported.add(it)
                }
            }
        view?.renderAccountList(asset, internal, imported)
    }

    private fun <T> Single<T>.showProgress() =
        this.doOnSubscribe { view?.showProgressDialog(com.blockchain.stringResources.R.string.please_wait) }
            .doAfterTerminate { view?.dismissProgressDialog() }

    private fun Completable.showProgress() =
        this.doOnSubscribe { view?.showProgressDialog(com.blockchain.stringResources.R.string.please_wait) }
            .doAfterTerminate { view?.dismissProgressDialog() }

    // TODO: Find a better way!
    private val SingleAccount.isInternalAccount: Boolean
        get() = (this as? BtcCryptoWalletAccount)?.isHDAccount
            ?: (this as? BchCryptoWalletAccount)?.let { true }
            ?: throw java.lang.IllegalStateException("Unexpected asset type")
}
