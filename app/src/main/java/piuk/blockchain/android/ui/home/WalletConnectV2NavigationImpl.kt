package piuk.blockchain.android.ui.home

import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.blockchain.commonarch.presentation.base.BlockchainActivity
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletConnectV2SessionProposal
import com.blockchain.walletconnect.ui.navigation.WalletConnectV2Navigation
import com.blockchain.walletconnect.ui.sessionapproval.WCApproveSessionBottomSheet
import com.blockchain.walletconnect.ui.sessionapproval.WCSessionUpdatedBottomSheet
import com.walletconnect.web3.wallet.client.Wallet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectV2NavigationImpl(
    private val activity: BlockchainActivity?,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag,
) : WalletConnectV2Navigation {

    private var walletConnectEventsTask: Job? = null

    override suspend fun launchWalletConnectV2() {
        Timber.d("Launching WalletConnect V2")
        if (walletConnectEventsTask == null && walletConnectV2FeatureFlag.coEnabled()) {

            require(activity != null)

            walletConnectV2Service.init()

            walletConnectEventsTask = activity.lifecycleScope.launch {
                walletConnectV2Service.walletEvents.flowWithLifecycle(activity.lifecycle)
                    .distinctUntilChanged()
                    .collectLatest {
                        processWalletConnectV2Event(it)
                    }
            }
        }
    }

    override fun approveOrRejectSession(dappName: String, dappDescription: String, dappLogoUrl: String) {
        activity!!.showBottomSheet(
            WCApproveSessionBottomSheet.newInstanceWalletConnectV2(
                WalletConnectV2SessionProposal(
                    dappName = dappName,
                    dappDescription = dappDescription,
                    dappLogoUrl = dappLogoUrl,
                )
            )
        )
    }

    override fun sessionApproveSuccess(dappName: String, dappLogoUrl: String) {
        activity!!.showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstanceV2SessionApproved(dappName = dappName, dappLogoUrl = dappLogoUrl)
        )
    }

    override fun sessionApproveFailed() {
        activity!!.showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstanceV2SessionRejected("", "")
        )
    }

    override fun sessionRejected(dappName: String, dappLogoUrl: String) {
        activity!!.showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstanceV2SessionRejected(dappName, dappLogoUrl)
        )
    }

    override fun sessionUnsupported(dappName: String, dappLogoUrl: String) {
        activity!!.showBottomSheet(
            WCSessionUpdatedBottomSheet.newInstanceV2SessionNotSupported(dappName, dappLogoUrl)
        )
    }

    private suspend fun processWalletConnectV2Event(event: Wallet.Model) {
        Timber.d("WalletConnect V2 Event: ${event.javaClass.simpleName}")
        when (event) {
            is Wallet.Model.SessionProposal -> {
                walletConnectV2Service.buildApprovedSessionNamespaces(event)
                    .catch {
                        Timber.e("Error building approved session namespaces ${it.message}")
                        sessionUnsupported(event.name, event.icons.first().toString())
                    }
                    .collectLatest {
                        approveOrRejectSession(
                            event.name,
                            event.description,
                            event.icons.first().toString()
                        )
                    }
            }
            is Wallet.Model.SettledSessionResponse.Result -> {
                sessionApproveSuccess(
                    dappName = event.session.metaData?.name.orEmpty(),
                    dappLogoUrl = event.session.metaData?.icons?.first().orEmpty()
                )
            }
            is Wallet.Model.SettledSessionResponse.Error -> {
                sessionApproveFailed()
            }
            is Wallet.Model.Error -> {
                sessionApproveFailed()
            }
            else -> {
                Timber.e("Unknown event $event")
            }
        }
    }
}
