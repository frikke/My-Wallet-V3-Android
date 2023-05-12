package com.blockchain.walletconnect.data
import android.app.Application
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TxResult
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.EVM_CHAIN_ROOT
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.EVM_CHAIN_ROOT_PREFIX
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SEND_RAW_TRANSACTION
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SEND_TRANSACTION
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN_TRANSACTION
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN_TYPED_DATA
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_PERSONAL_SIGN
import com.blockchain.walletconnect.domain.WalletConnectV2UrlValidator
import com.blockchain.walletconnect.ui.networks.ETH_CHAIN_ID
import com.blockchain.walletmode.WalletMode
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletConnectV2ServiceImpl(
    private val application: Application,
    private val ethDataManager: EthDataManager,
    private val coincore: Coincore,
    private val ethRequestSign: EthRequestSign,
    private val ethRequestSend: EthSendTransactionRequest
) : WalletConnectV2Service, WalletConnectV2UrlValidator, Web3Wallet.WalletDelegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _walletEvents: MutableSharedFlow<Wallet.Model> = MutableSharedFlow()
    override val walletEvents: SharedFlow<Wallet.Model> = _walletEvents.asSharedFlow()

    private val _walletConnectUserEvents: MutableSharedFlow<WalletConnectUserEvent> = MutableSharedFlow()
    override val userEvents: SharedFlow<WalletConnectUserEvent> = _walletConnectUserEvents.asSharedFlow()

    override fun init() {
        // WalletConnect V2 Initialization
        val projectId = "bcb13be5052677e9e7848634a68206ae" // TODO HARDCODED
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val connectionType = ConnectionType.AUTOMATIC

        CoreClient.initialize(
            relayServerUrl = serverUrl,
            connectionType = connectionType,
            application = application,
            metaData = Core.Model.AppMetaData(
                name = "Blockchain.com",
                description = "",
                url = "https://www.blockchain.com",
                icons = listOf("https://www.blockchain.com/static/apple-touch-icon.png"),
                redirect = null,
                verifyUrl = null
            ),
            relay = null,
            keyServerUrl = null,
            networkClientTimeout = null,
            onError = { error ->
                Timber.e("Core error: $error")
            },
        )

        val initParams = Wallet.Params.Init(CoreClient)
        Web3Wallet.initialize(
            initParams,
            onSuccess = {
                Web3Wallet.setWalletDelegate(this)
            },
            onError = { error ->
                Timber.e("Web3Wallet init error: $error")
            }
        )
    }

    override fun pair(pairingUrl: String) {
        Timber.d("WalletConnect V2: pairing with $pairingUrl")
        val pairingParams = Wallet.Params.Pair(pairingUrl)
        Web3Wallet.pair(pairingParams) { error ->
            scope.launch {
                Timber.e("Pairing error: $error")
                _walletEvents.emit(error)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun buildApprovedSessionNamespaces(
        sessionProposal: Wallet.Model.SessionProposal
    ): Flow<Map<String, Wallet.Model.Namespace.Session>> {
        val defaultEthReceiveAddressFlow = activeDeFiEtherAccounts().flatMapLatest { ethAccounts ->
            val defaultEthAccount = ethAccounts.first {
                it.isDefault
            }

            defaultEthAccount.receiveAddress.asFlow()
        }

        val supportedEvmNetworksFlow = getSupportedEvmNetworks().asFlow().map { supportedEvmNetworks ->
            supportedEvmNetworks.map {
                EVM_CHAIN_ROOT_PREFIX + "${it.chainId ?: ETH_CHAIN_ID}"
            }
        }

        return combine(defaultEthReceiveAddressFlow, supportedEvmNetworksFlow) { receiveAddress, supportedEvmChains ->

            Timber.d("WalletConnectV2: Supported EVM chains -> $supportedEvmChains")

            val addresses = supportedEvmChains.map { chain ->
                "$chain:${receiveAddress.address}"
            }
            val sessionNamespace = mapOf(
                EVM_CHAIN_ROOT to Wallet.Model.Namespace.Session(
                    chains = supportedEvmChains,
                    methods = getSupportedMethods(),
                    events = getSupportedEvents(),
                    accounts = addresses
                )
            )
            Web3Wallet.generateApprovedNamespaces(
                sessionProposal = sessionProposal, supportedNamespaces = sessionNamespace
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun approveLastSession() {
        Timber.d("WalletConnect V2: Approving last session")
        if (Web3Wallet.getSessionProposals().isNotEmpty()) {
            scope.launch {
                val sessionProposal = Web3Wallet.getSessionProposals().last()
                buildApprovedSessionNamespaces(sessionProposal).mapLatest { sessionNamespaces ->
                    Wallet.Params.SessionApprove(
                        proposerPublicKey = sessionProposal.proposerPublicKey,
                        namespaces = sessionNamespaces
                    )
                }.collectLatest { sessionApproval ->
                    Web3Wallet.approveSession(
                        sessionApproval,
                        onError = { error ->
                            Timber.e("WalletConnect V2: Approve session error: $error")
                        },
                        onSuccess = {
                            Timber.d("WalletConnect V2: Session approved: $it")
                        }
                    )
                }
            }
        }
    }

    override fun getSessions(): List<Wallet.Model.Session> =
        Web3Wallet.getListOfActiveSessions()

    override suspend fun ethSign(sessionRequest: Wallet.Model.SessionRequest) =
        ethRequestSign.onEthSignV2(
            sessionRequest,
            onTxCompleted = { txResult ->
                (txResult as? TxResult.HashedTxResult)?.let { hashedTxResult ->
                    Completable.fromCallable {
                        sessionRequestComplete(sessionRequest, hashedTxResult)
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                Completable.fromCallable {
                    sessionRequestFailed(sessionRequest)
                }
            }
        ).collectLatest {
            _walletConnectUserEvents.emit(it)
        }

    override suspend fun ethSend(sessionRequest: Wallet.Model.SessionRequest, method: String) =
        ethRequestSend.onSendTransactionV2(
            sessionRequest,
            method = method,
            onTxCompleted = { txResult ->
                (txResult as? TxResult.HashedTxResult)?.let { hashedTxResult ->
                    Completable.fromCallable {
                        sessionRequestComplete(sessionRequest, hashedTxResult)
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                Completable.fromCallable {
                    sessionRequestFailed(sessionRequest)
                }
            }
        ).collectLatest {
            _walletConnectUserEvents.emit(it)
        }

    override fun sessionRequestComplete(
        sessionRequest: Wallet.Model.SessionRequest,
        hashedTxResult: TxResult.HashedTxResult
    ) =
        Web3Wallet.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = sessionRequest.topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                    id = sessionRequest.request.id,
                    result = hashedTxResult.txId
                )
            ),
            onSuccess = {
                Timber.d("WalletConnect V2: Session request response: $it")
            },
            onError = { error ->
                Timber.e("WalletConnect V2: Session request response error: $error")
            }
        )

    override fun sessionRequestFailed(sessionRequest: Wallet.Model.SessionRequest) =
        Web3Wallet.respondSessionRequest(
            params = Wallet.Params.SessionRequestResponse(
                sessionTopic = sessionRequest.topic,
                jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                    id = sessionRequest.request.id, code = 500, message = "Transaction Cancelled"
                )
            ),
            onSuccess = {
                Timber.d("WalletConnect V2: Session request response: $it")
            },
            onError = { error ->
                Timber.e("WalletConnect V2: Session request response error: $error")
            }
        )

    override fun disconnectAllSessions() {
        Timber.d("Disconnect all sessions. Active Sessions: ${Web3Wallet.getListOfActiveSessions()}")

        Web3Wallet.getListOfActiveSessions().forEach { session ->
            Web3Wallet.disconnectSession(
                Wallet.Params.SessionDisconnect(session.topic),
                onSuccess = {
                    Timber.d("Session disconnected: $session")
                },
                onError = { error ->
                    Timber.e("Disconnect session error: $error")
                }
            )
        }
    }

    override fun disconnectSession(sessionTopic: String) {
        Web3Wallet.disconnectSession(
            params = Wallet.Params.SessionDisconnect(sessionTopic),
            onSuccess = {
                Timber.d("Session disconnected: $it")
            },
            onError = { error ->
                Timber.e("Disconnect session error: $error")
            }
        )
    }

    override fun clearSessionProposals() {
        Timber.e("clear Session Proposals")
        Web3Wallet.getSessionProposals().map { sessionProposal ->
            Web3Wallet.rejectSession(
                Wallet.Params.SessionReject(sessionProposal.proposerPublicKey, ""),
                onSuccess = {
                    Timber.d("Session rejected: $it")
                },
                onError = { error ->
                    Timber.e("Session rejected error: $error")
                }
            )
        }
    }

    override fun validateURI(uri: String): Boolean {
        val regex = Regex("^wc:[0-9a-fA-F]+@2\\?(.*)")
        return regex.matches(uri)
    }

    private fun getSupportedEvmNetworks() = ethDataManager.supportedNetworks

    private fun activeDeFiEtherAccounts() =
        coincore.activeWalletsInMode(
            walletMode = WalletMode.NON_CUSTODIAL,
        ).map {
            it.accounts
        }.map {
            it.filterIsInstance<CryptoAccount>()
        }.map { accounts ->
            accounts.filter { account ->
                account.currency == CryptoCurrency.ETHER
            }
        }.distinctUntilChanged()

    private fun getSupportedMethods() = listOf(
        WC_METHOD_PERSONAL_SIGN,
        WC_METHOD_ETH_SIGN,
        WC_METHOD_ETH_SIGN_TYPED_DATA,
        WC_METHOD_ETH_SIGN_TRANSACTION,
        WC_METHOD_ETH_SEND_TRANSACTION,
        WC_METHOD_ETH_SEND_RAW_TRANSACTION,
    )

    private fun getSupportedEvents() = listOf("chainChanged", "accountsChanged")

    override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
        // Triggered when a Dapp sends SessionProposal to connect
        Timber.d("(WalletConnect) Session proposal: $sessionProposal")
        scope.launch {
            _walletEvents.emit(sessionProposal)
        }
    }

    override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        Timber.d("(WalletConnect) Session request: $sessionRequest")

        scope.launch {
            when (sessionRequest.request.method) {
                WC_METHOD_PERSONAL_SIGN,
                WC_METHOD_ETH_SIGN -> ethSign(sessionRequest)
                WC_METHOD_ETH_SIGN_TYPED_DATA -> {
                    Timber.e("Typed data not supported yet")
                }
                WC_METHOD_ETH_SIGN_TRANSACTION,
                WC_METHOD_ETH_SEND_TRANSACTION -> ethSend(sessionRequest, sessionRequest.request.method)
            }
        }
    }

    override fun onAuthRequest(authRequest: Wallet.Model.AuthRequest) {
        // Triggered when Dapp / Requester makes an authorization request
        Timber.d("(WalletConnect) Auth request: $authRequest")
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        // Triggered when the session is deleted by the peer
        Timber.d("(WalletConnect) Session delete: $sessionDelete")
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        Timber.d("(WalletConnect) Session settle response: $settleSessionResponse")
        scope.launch {
            _walletEvents.emit(settleSessionResponse)
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
        // Triggered when wallet receives the session update response from Dapp
        Timber.d("(WalletConnect) Session update response: $sessionUpdateResponse")
    }

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
        // Triggered whenever the connection state is changed
        Timber.d("(WalletConnect) Connection state: $state")
    }

    override fun onError(error: Wallet.Model.Error) {
        // Triggered whenever there is an issue inside the SDK
        Timber.e("(WalletConnect) Wallet error: $error")
    }
}
