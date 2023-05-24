package com.blockchain.walletconnect.data
import android.app.Application
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TxResult
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionProposalState
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
import com.blockchain.walletconnect.domain.WalletInfo
import com.blockchain.walletconnect.ui.networks.ETH_CHAIN_ID
import com.blockchain.walletmode.WalletMode
import com.walletconnect.android.internal.common.scope
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.withContext
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

    init {
        val delegate = this
        scope.launch {
            Web3Wallet.setWalletDelegate(delegate)
        }
    }

    override suspend fun pair(pairingUrl: String) {
        Timber.d("WalletConnect V2: pairing with $pairingUrl")
        withContext(Dispatchers.IO) {
            val pairingParams = Wallet.Params.Pair(pairingUrl)
            Web3Wallet.pair(
                pairingParams,
                onSuccess = {
                    Timber.d("WalletConnect V2: pairing success")
                },
                onError = { error ->
                    scope.launch {
                        Timber.e("WalletConnect V2: pairing error: $error")
                        _walletEvents.emit(error)
                    }
                },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun buildApprovedSessionNamespaces(
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

    // TODO(labreu): take in a topicID so that the correct session is approved
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun approveLastSession() {
        Timber.d("WalletConnect V2: Approving last session")
        scope.launch {
            if (Web3Wallet.getSessionProposals().isNotEmpty()) {
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

    override suspend fun getSessions(): List<WalletConnectSession> =
        withContext(Dispatchers.IO) {
            Web3Wallet.getListOfActiveSessions().map {
                it.toDomainWalletConnectSession()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSessionsFlow(): Flow<List<WalletConnectSession>> =
        _walletEvents.transformLatest { event ->
            if (event is Wallet.Model.SettledSessionResponse || event is Wallet.Model.SessionDelete) {
                Timber.d("WalletConnect V2: Emitting updated list of sessions")
                emit(getSessions())
            }
        }.onStart { // Emit the initial list of sessions when the flow is first collected
            Timber.d("WalletConnect V2: Emitting initial list of sessions")
            emit(getSessions())
        }.distinctUntilChanged() // Ensure that the same list is not emitted multiple times

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSessionProposalState(): Flow<WalletConnectSessionProposalState?> {
        return _walletEvents.transformLatest { event ->
            when (event) {
                is Wallet.Model.SettledSessionResponse.Result -> {
                    emit(WalletConnectSessionProposalState.APPROVED)
                }
                is Wallet.Model.SettledSessionResponse.Error -> {
                    emit(WalletConnectSessionProposalState.REJECTED)
                }
                else -> emit(null)
            }
        }.onStart {
            emit(null)
        }.distinctUntilChanged()
    }

    override suspend fun getSession(sessionId: String): WalletConnectSession? =
        withContext(Dispatchers.IO) {
            Web3Wallet.getActiveSessionByTopic(sessionId)?.toDomainWalletConnectSession()
        }

    override suspend fun ethSign(sessionRequest: Wallet.Model.SessionRequest) =
        ethRequestSign.onEthSignV2(
            sessionRequest,
            onTxCompleted = { txResult ->
                (txResult as? TxResult.HashedTxResult)?.let { hashedTxResult ->
                    rxCompletable {
                        sessionRequestComplete(sessionRequest, hashedTxResult)
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                rxCompletable {
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
                    rxCompletable {
                        sessionRequestComplete(sessionRequest, hashedTxResult)
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                rxCompletable {
                    sessionRequestFailed(sessionRequest)
                }
            }
        ).collectLatest {
            _walletConnectUserEvents.emit(it)
        }

    override suspend fun sessionRequestComplete(
        sessionRequest: Wallet.Model.SessionRequest,
        hashedTxResult: TxResult.HashedTxResult
    ) = withContext(Dispatchers.IO) {
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
    }

    override suspend fun sessionRequestFailed(sessionRequest: Wallet.Model.SessionRequest) =
        withContext(Dispatchers.IO) {
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
        }

    override suspend fun disconnectAllSessions() {
        Timber.d("WalletConnect V2: Disconnecting all sessions")
        withContext(Dispatchers.IO) {
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
    }

    override suspend fun disconnectSession(sessionTopic: String) {
        withContext(Dispatchers.IO) {
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
    }

    override suspend fun getSessionProposal(sessionId: String): Wallet.Model.SessionProposal? =
        withContext(Dispatchers.IO) {
            Web3Wallet.getSessionProposals().firstOrNull { it.pairingTopic == sessionId }
        }

    override fun clearSessionProposals() {
        Timber.d("WalletConnect V2: Clearing Session Proposals")
        scope.launch {
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
        scope.launch {
            _walletEvents.emit(sessionDelete)
        }
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

private fun Wallet.Model.Session.toDomainWalletConnectSession(): WalletConnectSession =
    WalletConnectSession(
        url = this.metaData?.redirect.orEmpty(),
        dAppInfo = DAppInfo(
            peerId = "", // This can be empty
            peerMeta = ClientMeta(
                description = this.metaData?.description.orEmpty(),
                url = this.metaData?.url.orEmpty(),
                icons = this.metaData?.icons.orEmpty(),
                name = this.metaData?.name.orEmpty()
            ),
            chainId = ETH_CHAIN_ID,
        ),
        walletInfo = WalletInfo(
            clientId = this.topic,
            sourcePlatform = "Android"
        ),
        isV2 = true
    )
