package piuk.blockchain.android.ui.home

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoTarget
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.home.presentation.navigation.QrExpected
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.ScanResult
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.getOrNull
import com.blockchain.utils.awaitOutcome
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.awaitSingle
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.ui.auth.newlogin.domain.service.SecureChannelService
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class QrScanNavigationImpl(
    private val activity: BlockchainActivity,
    private val qrScanResultProcessor: QrScanResultProcessor,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val secureChannelService: SecureChannelService
) : QrScanNavigation {

    private lateinit var resultLauncher: ActivityResultLauncher<Set<QrExpected>>

    override fun registerForQrScan(onScan: (String) -> Unit): ActivityResultLauncher<Set<QrExpected>> {
        resultLauncher = activity.registerForActivityResult(QrScanActivityContract(), onScan)
        return resultLauncher
    }

    override fun launchQrScan() {
        resultLauncher.launch(QrExpected.MAIN_ACTIVITY_QR)
    }

    override fun processQrResult(decodedData: String) {
        activity.lifecycleScope.launch {
            qrScanResultProcessor.processScan(decodedData).awaitOutcome()
                .doOnFailure {
                    // TODO: error handling
                    Timber.e(it)
                }
                .getOrNull()?.let { scanResult ->
                    processResult(scanResult)
                }
        }
    }

    private suspend fun launchTxFlowWithTarget(target: CryptoTarget) {
        try {
            val sourceAccount = qrScanResultProcessor.selectSourceAccount(activity, target).awaitSingle()
            activity.startActivity(
                TransactionFlowActivity.newIntent(
                    context = activity,
                    sourceAccount = sourceAccount,
                    target = target,
                    action = AssetAction.Send
                )
            )
        } catch (ex: Exception) {
            Timber.e(ex)
//            BlockchainSnackbar.make(
//                activity,
//                activity.getString(R.string.scan_no_available_account, target.asset.displayTicker)
//            )
        }
    }

    private suspend fun disambiguateSendScan(targets: Collection<CryptoTarget>): Outcome<Exception, CryptoTarget> {
        return qrScanResultProcessor.disambiguateScan(activity, targets).awaitOutcome()
    }

    private suspend fun processResult(scanResult: ScanResult) {
        when (scanResult) {
            is ScanResult.HttpUri -> {
                // TODO: handle deeplinking
            }
            is ScanResult.ImportedWallet -> {
                // TODO: as part of Auth
            }
            is ScanResult.SecuredChannelLogin -> {
                secureChannelService.sendHandshake(scanResult.handshake)
            }
            is ScanResult.TxTarget -> {
                if (scanResult.targets.size > 1) {
                    disambiguateSendScan(scanResult.targets)
                        .doOnFailure {
                            // TODO: handle error
                            Timber.e(it)
                        }
                        .getOrNull()?.let { cryptoTarget ->
                            launchTxFlowWithTarget(cryptoTarget)
                        }
                } else if (scanResult.targets.size == 1) {
                    launchTxFlowWithTarget(scanResult.targets.first())
                }
            }
            is ScanResult.WalletConnectRequest -> {
                // TODO: error handling AND-6837
                walletConnectServiceAPI.attemptToConnect(scanResult.data).awaitOutcome()
                    .doOnFailure { Timber.e(it) }
            }
        }
    }
}
