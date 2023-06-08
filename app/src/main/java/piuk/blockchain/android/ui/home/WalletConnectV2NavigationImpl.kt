package piuk.blockchain.android.ui.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.NavArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.navigate
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.ui.navigation.WalletConnectDestination
import com.blockchain.walletconnect.ui.navigation.WalletConnectV2Navigation
import com.walletconnect.web3.wallet.client.Wallet
import java.net.URLEncoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectV2NavigationImpl(
    private val lifecycle: Lifecycle,
    private val navController: NavHostController,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag,
) : WalletConnectV2Navigation {

    private var walletConnectEventsTask: Job? = null

    override fun launchWalletConnectV2() {
        Timber.d("Launching WalletConnect V2")
        if (walletConnectEventsTask == null) {
            walletConnectEventsTask = lifecycle.coroutineScope.launch {
                if (walletConnectV2FeatureFlag.coEnabled()) {
                    walletConnectV2Service.walletEvents.flowWithLifecycle(lifecycle)
                        .distinctUntilChanged()
                        .collectLatest {
                            processWalletConnectV2Event(it)
                        }
                }
            }
        }
    }

    override fun approveOrRejectSession(sessionId: String, walletAddress: String) {
        navController.navigate(
            WalletConnectDestination.WalletConnectSessionProposal,
            listOfNotNull(
                NavArgument(key = WalletConnectDestination.ARG_SESSION_ID, value = sessionId),
                NavArgument(key = WalletConnectDestination.ARG_WALLET_ADDRESS, value = walletAddress)
            ),
        )
    }

    override fun approveSession() {
        walletConnectV2Service.approveLastSession()
    }

    override fun rejectSession() {
        walletConnectV2Service.clearSessionProposals()
    }

    override fun sessionUnsupported(dappName: String, dappLogoUrl: String) {
        navController.navigate(
            WalletConnectDestination.WalletConnectSessionNotSupported,
            listOfNotNull(
                NavArgument(
                    key = WalletConnectDestination.ARG_DAPP_NAME,
                    value = dappName
                ),
                NavArgument(
                    key = WalletConnectDestination.ARG_DAPP_LOGO_URL,
                    value = URLEncoder.encode(dappLogoUrl, "UTF-8")
                )
            )
        )
    }

    private suspend fun processWalletConnectV2Event(event: Wallet.Model) {
        Timber.d("WalletConnect V2 Event: ${event.javaClass.simpleName}")
        when (event) {
            is Wallet.Model.SessionProposal -> {
                walletConnectV2Service.buildApprovedSessionNamespaces(event)
                    .catch {
                        Timber.e("Error building approved session namespaces ${it.message}")
                        sessionUnsupported(event.name, event.icons.firstOrNull()?.toString().orEmpty())
                    }
                    .collectLatest {
                        approveOrRejectSession(event.pairingTopic, event.proposerPublicKey)
                    }
            }
            is Wallet.Model.AuthRequest -> {
                navController.navigate(
                    WalletConnectDestination.WalletConnectAuthRequest,
                    listOfNotNull(
                        NavArgument(
                            key = WalletConnectDestination.ARG_AUTH_ID,
                            value = event.id
                        )
                    )
                )
            }
            else -> {
                Timber.e("Unknown event $event")
            }
        }
    }
}
