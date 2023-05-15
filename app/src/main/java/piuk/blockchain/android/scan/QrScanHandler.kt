package piuk.blockchain.android.scan

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.blockchain.analytics.Analytics
import com.blockchain.bitpay.BITPAY_LIVE_BASE
import com.blockchain.bitpay.BitPayDataManager
import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.bitpay.PATH_BITPAY_INVOICE
import com.blockchain.coincore.AddressFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.filterByAction
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.navigation.ScanResult
import com.blockchain.koin.payloadScope
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.domain.WalletConnectV2UrlValidator
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.FormatsUtil.BCH_PREFIX
import info.blockchain.wallet.util.FormatsUtil.BTC_PREFIX
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.MaybeSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.account.AccountSelectSheet
import piuk.blockchain.android.ui.scan.CameraAnalytics
import piuk.blockchain.android.ui.scan.QrCodeType

class QrScanError(val errorCode: ErrorCode, msg: String) : Exception(msg) {
    enum class ErrorCode {
        ScanUnrecognized, // Scanned successfully but unrecognized url
        ScanFailed, // General Purpose Error. The most common case for now until scan gets overhauled
        BitPayScanFailed
    }
}

class QrScanResultProcessor(
    private val bitPayDataManager: BitPayDataManager,
    private val walletConnectUrlValidator: WalletConnectUrlValidator,
    private val walletConnectV2UrlValidator: WalletConnectV2UrlValidator,
    private val analytics: Analytics
) {

    fun processScan(scanResult: String, isDeeplinked: Boolean = false): Single<out ScanResult> =
        when {
            scanResult.isHttpUri() -> {
                if (scanResult.isUriRecognizable()) {
                    Single.just(ScanResult.HttpUri(scanResult, isDeeplinked))
                } else {
                    Single.error(QrScanError(QrScanError.ErrorCode.ScanUnrecognized, "Unrecognized QR"))
                }
            }
            scanResult.isBitpayUri() -> parseBitpayInvoice(scanResult)
                .map {
                    ScanResult.TxTarget(setOf(it), isDeeplinked)
                }
            scanResult.isJson() -> Single.just(ScanResult.SecuredChannelLogin(scanResult))
            walletConnectUrlValidator.isUrlValid(scanResult) ->
                Single.just(ScanResult.WalletConnectRequest(scanResult))
            walletConnectV2UrlValidator.validateURI(scanResult) ->
                Single.just(ScanResult.WalletConnectV2Request(scanResult))
            else -> {
                val addressParser: AddressFactory = payloadScope.get()
                addressParser.parse(scanResult)
                    .onErrorResumeNext {
                        Single.error(QrScanError(QrScanError.ErrorCode.ScanFailed, it.message ?: "Unknown reason"))
                    }.flatMap {
                        if (it.isEmpty()) {
                            Single.error(
                                QrScanError(QrScanError.ErrorCode.ScanUnrecognized, "Unrecognized QR")
                            )
                        } else {
                            Single.just(
                                ScanResult.TxTarget(
                                    it.filterIsInstance<CryptoAddress>().toSet(),
                                    isDeeplinked
                                )
                            )
                        }
                    }
            }
        }.doOnSuccess { scan ->
            analytics.logEvent(CameraAnalytics.QrCodeScanned(scan.type()))
        }.doOnError {
            analytics.logEvent(CameraAnalytics.QrCodeScanned(QrCodeType.INVALID))
        }

    private fun parseBitpayInvoice(bitpayUri: String): Single<CryptoTarget> {
        val cryptoCurrency = bitpayUri.getAssetFromLink()
        return BitPayInvoiceTarget.fromLink(cryptoCurrency, bitpayUri, bitPayDataManager)
            .onErrorResumeNext {
                Single.error(QrScanError(QrScanError.ErrorCode.BitPayScanFailed, it.message ?: "Unknown reason"))
            }
    }

    fun disambiguateScan(
        activity: Activity,
        targets: Collection<CryptoTarget>
    ): Single<CryptoTarget> {
        // TEMP while refactoring - replace with bottom sheet.
        val optionsList = ArrayList(targets)
        val selectList = optionsList.map {
            it.asset.name
        }.toTypedArray()

        val subject = SingleSubject.create<CryptoTarget>()

        AlertDialog.Builder(activity, com.blockchain.componentlib.R.style.AlertDialogStyle)
            .setTitle(com.blockchain.stringResources.R.string.confirm_currency)
            .setCancelable(true)
            .setSingleChoiceItems(
                selectList,
                -1
            ) { dialog, which ->
                dialog.dismiss()
                subject.onSuccess(optionsList[which])
            }
            .create()
            .show()

        return subject
    }

    fun selectAssetTargetFromScan(
        asset: AssetInfo,
        scanResult: ScanResult
    ): Maybe<CryptoAddress> =
        Maybe.just(scanResult)
            .filter { r -> r is ScanResult.TxTarget }
            .map { r ->
                (r as ScanResult.TxTarget).targets
                    .filterIsInstance<CryptoAddress>()
                    .first { a -> a.asset == asset }
            }.onErrorComplete()

    // TODO: Move this into the flow.
    // To not be a hack, this needs the TxTarget interface relationships
    // to be updated so that we can tell an internal target (account) from and external target (address)
    // there is a similar requirement elsewhere in the flow that as a commented workaround.
    @SuppressLint("CheckResult")
    fun selectSourceAccount(
        activity: BlockchainActivity,
        target: CryptoTarget
    ): Maybe<CryptoAccount> {
        val subject = MaybeSubject.create<CryptoAccount>()

        val asset = target.asset
        val coincore = payloadScope.get<Coincore>()

        coincore[asset].accountGroup(if (target is BitPayInvoiceTarget) AssetFilter.NonCustodial else AssetFilter.All)
            .map { group -> group.accounts }
            .defaultIfEmpty(emptyList())
            .flatMap { list -> list.filterByAction(AssetAction.Send) }
            .subscribeBy(
                onSuccess = { accounts ->
                    when (accounts.size) {
                        1 -> subject.onSuccess(accounts[0] as CryptoAccount)
                        0 -> subject.onComplete()
                        else -> showAccountSelectionDialog(
                            activity,
                            subject,
                            Single.just(accounts)
                        )
                    }
                },
                onError = {
                    subject.onError(it)
                }
            )
        return subject
    }

    private fun showAccountSelectionDialog(
        activity: BlockchainActivity,
        subject: MaybeSubject<CryptoAccount>,
        source: Single<SingleAccountList>
    ) {
        val selectionHost = object : AccountSelectSheet.SelectionHost {
            override fun onAccountSelected(account: BlockchainAccount) {
                subject.onSuccess(account as CryptoAccount)
            }

            override fun onSheetClosed() {
                if (!subject.hasValue()) {
                    subject.onComplete()
                }
            }
        }

        activity.showBottomSheet(
            AccountSelectSheet.newInstance(
                selectionHost,
                source.map { list ->
                    list.map { it as CryptoAccount }
                },
                com.blockchain.stringResources.R.string.select_send_source_title
            )
        )
    }
}

private fun ScanResult.type(): QrCodeType {
    return when (this) {
        is ScanResult.HttpUri -> QrCodeType.DEEPLINK
        is ScanResult.WalletConnectV2Request,
        is ScanResult.WalletConnectRequest -> QrCodeType.DAPP
        is ScanResult.SecuredChannelLogin -> QrCodeType.LOG_IN
        is ScanResult.TxTarget,
        is ScanResult.ImportedWallet -> QrCodeType.CRYPTO_ADDRESS
    }
}

private fun String.isHttpUri(): Boolean = this.startsWith("http")

private fun String.isUriRecognizable(): Boolean = Uri.parse(this).getQueryParameter("link") != null

private const val bitpayInvoiceUrl = "$BITPAY_LIVE_BASE$PATH_BITPAY_INVOICE/"

private fun String.isBitpayUri(): Boolean =
    FormatsUtil.getPaymentRequestUrl(this).contains(bitpayInvoiceUrl)

private fun String.getAssetFromLink(): AssetInfo =
    when {
        this.startsWith(BTC_PREFIX) -> CryptoCurrency.BTC
        this.startsWith(BCH_PREFIX) -> CryptoCurrency.BCH
        else -> throw IllegalArgumentException("$this cannot be mapped to a supported CryptoCurrency")
    }

private fun String.isJson(): Boolean = FormatsUtil.isValidJson(this)
