package piuk.blockchain.android.ui.home

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.loader.DynamicAssetsService
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.componentlib.alert.BlockchainSnackbar
import com.blockchain.domain.auth.SecureChannelService
import com.blockchain.home.presentation.navigation.QrExpected
import com.blockchain.home.presentation.navigation.QrScanNavigation
import com.blockchain.home.presentation.navigation.ScanResult
import com.blockchain.home.presentation.navigation.WCSessionIntent
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.fold
import com.blockchain.outcome.getOrNull
import com.blockchain.utils.awaitOutcome
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionEvent
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.ui.networks.NetworkInfo
import com.blockchain.walletconnect.ui.sessionapproval.WCApproveSessionBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCSessionUpdatedBottomSheet
import com.google.android.material.snackbar.Snackbar
import info.blockchain.balance.AssetCatalogue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.awaitSingle
import piuk.blockchain.android.R
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFlowActivity
import timber.log.Timber

class QrScanNavigationImpl(
    private val activity: BlockchainActivity?,
    private val qrScanResultProcessor: QrScanResultProcessor,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val secureChannelService: SecureChannelService,
    private val assetService: DynamicAssetsService,
    private val assetCatalogue: AssetCatalogue,
    private val walletConnectV2Service: WalletConnectV2Service
) : QrScanNavigation {

    private var resultLauncher: ActivityResultLauncher<Set<QrExpected>>?

    private var walletConnectEventsTask: Job? = null
    private var qrResultProcessorTask: Job? = null
    private var wcSessionProcessorTask: Job? = null

    init {
        resultLauncher = activity?.registerForActivityResult(QrScanActivityContract()) {
            processQrResult(it)
        }
    }

    override fun launchQrScan() {
        if (walletConnectEventsTask == null) {
            // Only launch it once
            walletConnectEventsTask = activity!!.lifecycleScope.launch {
                walletConnectServiceAPI.sessionEvents.asFlow()
                    .collect { sessionEvent ->
                        processWC(sessionEvent)
                    }
            }
        }
        resultLauncher!!.launch(QrExpected.MAIN_ACTIVITY_QR)
    }

    override fun processQrResult(decodedData: String) {
        qrResultProcessorTask?.cancel()
        qrResultProcessorTask = activity!!.lifecycleScope.launch {
            qrScanResultProcessor.processScan(decodedData).awaitOutcome()
                .doOnFailure {
                    Timber.e(it)
                    BlockchainSnackbar.make(
                        activity.window.decorView.rootView,
                        activity.getString(com.blockchain.stringResources.R.string.scan_failed),
                        duration = Snackbar.LENGTH_SHORT
                    )
                }
                .getOrNull()?.let { scanResult ->
                    processResult(scanResult)
                }
        }
    }

    override fun updateWalletConnectSession(wcIntent: WCSessionIntent) {
        wcSessionProcessorTask?.cancel()
        wcSessionProcessorTask = activity!!.lifecycleScope.launch {
            when (wcIntent) {
                is WCSessionIntent.ApproveWCSession ->
                    walletConnectServiceAPI.acceptConnection(wcIntent.session).awaitOutcome()
                is WCSessionIntent.GetNetworkInfoForWCSession -> getNetworkInfoForWCSession(wcIntent.session)
                is WCSessionIntent.RejectWCSession ->
                    walletConnectServiceAPI.denyConnection(wcIntent.session).awaitOutcome()
                is WCSessionIntent.StartWCSession -> walletConnectServiceAPI.attemptToConnect(wcIntent.url)
                    .awaitOutcome()
            }
        }
    }

    override fun unregister() {
        resultLauncher?.unregister()
    }

    private suspend fun launchTxFlowWithTarget(target: CryptoTarget) {
        try {
            val sourceAccount = qrScanResultProcessor.selectSourceAccount(activity!!, target).awaitSingle()
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
            BlockchainSnackbar.make(
                activity!!.window.decorView.rootView,
                activity.getString(
                    com.blockchain.stringResources.R.string.scan_no_available_account,
                    target.asset.displayTicker
                )
            )
        }
    }

    private suspend fun disambiguateSendScan(targets: Collection<CryptoTarget>): Outcome<Exception, CryptoTarget> {
        return qrScanResultProcessor.disambiguateScan(activity!!, targets).awaitOutcome()
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
                            Timber.e(it)
                            BlockchainSnackbar.make(
                                activity!!.window.decorView.rootView,
                                activity.getString(com.blockchain.stringResources.R.string.scan_failed),
                                duration = Snackbar.LENGTH_SHORT
                            )
                        }
                        .getOrNull()?.let { cryptoTarget ->
                            launchTxFlowWithTarget(cryptoTarget)
                        }
                } else if (scanResult.targets.size == 1) {
                    launchTxFlowWithTarget(scanResult.targets.first())
                }
            }
            is ScanResult.WalletConnectRequest -> {
                walletConnectServiceAPI.attemptToConnect(scanResult.data)
                    .awaitOutcome()
                    .doOnFailure { Timber.e(it) }
            }
            is ScanResult.WalletConnectV2Request ->
                walletConnectV2Service.pair(scanResult.data)
        }
    }

    private suspend fun processWC(sessionEvent: WalletConnectSessionEvent) {
        when (sessionEvent) {
            is WalletConnectSessionEvent.DidConnect -> {
                activity!!.showBottomSheet(
                    WCSessionUpdatedBottomSheet.newInstance(session = sessionEvent.session, approved = true)
                )
            }
            is WalletConnectSessionEvent.DidDisconnect -> {
                Timber.i("Session ${sessionEvent.session.url} Disconnected")
            }
            is WalletConnectSessionEvent.DidReject,
            is WalletConnectSessionEvent.FailToConnect -> {
                activity!!.showBottomSheet(
                    WCSessionUpdatedBottomSheet.newInstance(session = sessionEvent.session, approved = false)
                )
            }
            is WalletConnectSessionEvent.ReadyForApproval -> {
                getNetworkInfoForWCSession(sessionEvent.session)
            }
        }
    }

    private suspend fun getNetworkInfoForWCSession(session: WalletConnectSession) =
        assetService.allEvmNetworks().awaitOutcome()
            .fold(
                onFailure = {
                    Timber.e(it)
                    activity!!.showBottomSheet(
                        WCApproveSessionBottomSheet.newInstance(session)
                    )
                },
                onSuccess = { networks ->
                    val network = networks.find { it.chainId == session.dAppInfo.chainId }
                    network?.let {
                        val networkInfo = NetworkInfo(
                            networkTicker = network.networkTicker,
                            name = network.name,
                            chainId = network.chainId!!,
                            logo = assetCatalogue.assetInfoFromNetworkTicker(network.networkTicker)?.logo
                        )
                        activity!!.showBottomSheet(
                            WCApproveSessionBottomSheet.newInstance(
                                session,
                                networkInfo
                            )
                        )
                    } ?: activity!!.showBottomSheet(WCApproveSessionBottomSheet.newInstance(session))
                }
            )
}
