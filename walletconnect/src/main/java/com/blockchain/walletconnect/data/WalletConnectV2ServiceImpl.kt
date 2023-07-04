package com.blockchain.walletconnect.data

import android.app.Application
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.TxResult
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.chains.ethereum.EthMessageSigner
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.payloadScope
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.DappRedirectUri
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.WalletConnectAuthSigningPayload
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionProposalState
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.EVM_CHAIN_ROOT
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.EVM_CHAIN_ROOT_PREFIX
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.ISS_DID_PREFIX
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
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.cacao.signature.SignatureType
import com.walletconnect.android.internal.common.signing.cacao.Cacao
import com.walletconnect.android.internal.common.signing.signature.Signature
import com.walletconnect.android.internal.common.signing.signature.toCacaoSignature
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.withContext
import org.bouncycastle.util.encoders.Hex
import timber.log.Timber

class WalletConnectV2ServiceImpl(
    private val lifecycleObservable: LifecycleObservable,
    private val walletConnectV2FeatureFlag: FeatureFlag
) :
    WalletConnectV2Service,
    WalletConnectV2UrlValidator,
    Web3Wallet.WalletDelegate {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _walletEvents: MutableSharedFlow<Wallet.Model> = MutableSharedFlow()
    override val walletEvents: SharedFlow<Wallet.Model> = _walletEvents.asSharedFlow()

    private val _walletConnectUserEvents: MutableSharedFlow<WalletConnectUserEvent> = MutableSharedFlow()
    override val userEvents: SharedFlow<WalletConnectUserEvent> = _walletConnectUserEvents.asSharedFlow()

    private val _dappRedirectEvents: MutableSharedFlow<DappRedirectUri> = MutableSharedFlow()
    override val dappRedirectEvents: SharedFlow<DappRedirectUri> = _dappRedirectEvents.asSharedFlow()

    private val ethDataManager: EthDataManager
        get() = payloadScope.get()

    private val coincore: Coincore
        get() = payloadScope.get()

    private val ethRequestSign: EthRequestSign
        get() = payloadScope.get()

    private val ethRequestSend: EthSendTransactionRequest
        get() = payloadScope.get()

    private val ethMessageSigner: EthMessageSigner
        get() = payloadScope.get()

    override fun initWalletConnect(application: Application, projectId: String, relayUrl: String) {
        // WalletConnect V2 Initialization
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val connectionType = ConnectionType.MANUAL
        scope.launch(Dispatchers.IO) {
            if (!walletConnectV2FeatureFlag.coEnabled()) return@launch
            try {
                CoreClient.initialize(
                    relayServerUrl = serverUrl,
                    connectionType = connectionType,
                    application = application,
                    metaData = Core.Model.AppMetaData(
                        name = "Blockchain.com",
                        description = "",
                        url = "https://www.blockchain.com",
                        icons = listOf("https://www.blockchain.com/static/apple-touch-icon.png"),
                        redirect = "bc-dapp-request://",
                        verifyUrl = null
                    ),
                    relay = null,
                    keyServerUrl = null,
                    networkClientTimeout = null,
                    onError = { error ->
                        Timber.e("WalletConnect V2: Core error: $error")
                    },

                )

                val initParams = Wallet.Params.Init(CoreClient)
                Web3Wallet.initialize(
                    initParams,
                    onSuccess = {
                        Timber.d("WalletConnect V2: Web3Wallet init success")
                        Web3Wallet.setWalletDelegate(this@WalletConnectV2ServiceImpl)
                    },
                    onError = { error ->
                        Timber.e("WalletConnect V2: Web3Wallet init error: $error")
                    }
                )
            } catch (t: Throwable) {
                Timber.e(t, "WalletConnect V2: Web3Wallet exception")
            }
        }

        scope.launch {

            if (!walletConnectV2FeatureFlag.coEnabled()) return@launch

            lifecycleObservable.onStateUpdated.asFlow().collectLatest { appState ->
                if (appState == AppState.BACKGROUNDED) {
                    Timber.d("WalletConnect V2: App is in background, disconnecting")
                    CoreClient.Relay.disconnect { error: Core.Model.Error ->
                        Timber.e("WalletConnect V2: Disconnect Relay error: $error")
                    }
                }
            }
        }
    }

    override fun resumeConnection() {
        Timber.d("WalletConnect V2: resuming connection")
        CoreClient.Relay.connect { error: Core.Model.Error ->
            Timber.e("WalletConnect V2: Connect Relay error: $error")
        }
    }

    override suspend fun pair(pairingUrl: String) {
        Timber.d("WalletConnect V2: pairing with $pairingUrl")
        withContext(Dispatchers.IO) {

            clearSessionProposals()

            val pairingParams = Wallet.Params.Pair(pairingUrl)
            try {
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
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: pairing error: $e")
                _walletEvents.emit(Wallet.Model.Error(e))
            }
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
            try {
                Web3Wallet.generateApprovedNamespaces(
                    sessionProposal = sessionProposal, supportedNamespaces = sessionNamespace
                )
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: generateApprovedNamespaces error: $e")
                emptyMap()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun approveSession(sessionId: String) {
        Timber.d("WalletConnect V2: Approving last session")
        scope.launch {
            try {
                Web3Wallet.getSessionProposals().find { it.pairingTopic == sessionId }?.let {
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
                                if (sessionProposal.redirect.isNotEmpty()) {
                                    redirectToDapp(sessionProposal.redirect)
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: Approve session error: $e")
            }
        }
    }

    override suspend fun getSessions(): List<WalletConnectSession> =
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.getListOfActiveSessions().map {
                    it.toDomainWalletConnectSession()
                }
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: Error getting sessions: $e")
                emptyList()
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
    override fun getSessionProposalState(sessionId: String): Flow<WalletConnectSessionProposalState?> {
        return _walletEvents.transformLatest { event ->
            when (event) {
                is Wallet.Model.SettledSessionResponse.Result -> {
                    if (event.session.pairingTopic == sessionId) {
                        emit(WalletConnectSessionProposalState.APPROVED)
                    } else {
                        emit(null)
                    }
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
            try {
                Web3Wallet.getActiveSessionByTopic(sessionId)?.toDomainWalletConnectSession()
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: Error getting session: $e")
                null
            }
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
                sessionRequest.peerMetaData?.redirect?.let { redirectUri ->
                    if (redirectUri.isNotEmpty()) {
                        redirectToDapp(redirectUri)
                    }
                }
            },
            onError = { error ->
                Timber.e("WalletConnect V2: Session request response error: $error")
            }
        )
    }

    override suspend fun sessionRequestFailed(sessionRequest: Wallet.Model.SessionRequest) =
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.respondSessionRequest(
                    params = Wallet.Params.SessionRequestResponse(
                        sessionTopic = sessionRequest.topic,
                        jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                            id = sessionRequest.request.id, code = 500, message = "Transaction Cancelled"
                        )
                    ),
                    onSuccess = {
                        Timber.d("WalletConnect V2: Session request failed response: $it")
                        sessionRequest.peerMetaData?.redirect?.let { redirectUri ->
                            if (redirectUri.isNotEmpty()) {
                                redirectToDapp(redirectUri)
                            }
                        }
                    },
                    onError = { error ->
                        Timber.e("WalletConnect V2: Session request failed response error: $error")
                    }
                )
            } catch (e: Exception) {
                Timber.e("WalletConnect V2: Session request response error: $e")
            }
        }

    override fun redirectToDapp(redirectUri: DappRedirectUri) {
        scope.launch {
            _dappRedirectEvents.emit(redirectUri)
        }
    }

    override suspend fun disconnectAllSessions() {
        Timber.d("WalletConnect V2: Disconnecting all sessions")
        withContext(Dispatchers.IO) {
            try {
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
            } catch (e: Exception) {
                Timber.e("Disconnect all sessions error: $e")
            }
        }
    }

    override suspend fun disconnectSession(sessionTopic: String) {
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.disconnectSession(
                    params = Wallet.Params.SessionDisconnect(sessionTopic),
                    onSuccess = {
                        Timber.d("Session disconnected: $it")
                    },
                    onError = { error ->
                        Timber.e("Disconnect session error: $error")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Disconnect session error: $e")
            }
        }
    }

    override suspend fun getSessionProposal(sessionId: String): Wallet.Model.SessionProposal? =
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.getSessionProposals().firstOrNull { it.pairingTopic == sessionId }
            } catch (e: Exception) {
                Timber.e("Get session proposal error: $e")
                null
            }
        }

    override fun clearSessionProposals() {
        Timber.d("WalletConnect V2: Clearing Session Proposals")
        scope.launch {
            try {
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
            } catch (e: Exception) {
                Timber.e("Clear session proposals error: $e")
            }
        }
    }

    override suspend fun getAuthRequest(authId: String): Wallet.Model.PendingAuthRequest? =
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.getPendingAuthRequests().firstOrNull { it.id == authId.toLong() }
            } catch (e: Exception) {
                Timber.e("Get auth request error: $e")
                null
            }
        }

    override suspend fun approveAuthRequest(authSigningPayload: WalletConnectAuthSigningPayload) {
        withContext(Dispatchers.IO) {

            val messageHex = Hex.toHexString(authSigningPayload.authMessage.toByteArray(Charsets.UTF_8))

            // Sign the Ethereum message
            ethMessageSigner.signEthMessage(messageHex).asFlow().collectLatest { signature ->
                val r = signature.copyOfRange(0, 32)
                val s = signature.copyOfRange(32, 64)
                val v = signature.copyOfRange(64, 65)

                val web3WalletSignature = Signature(v, r, s)
                val cacaoSignature = Cacao.Signature(
                    SignatureType.EIP191.header,
                    web3WalletSignature.toCacaoSignature()
                )
                try {
                    Web3Wallet.respondAuthRequest(
                        params = Wallet.Params.AuthRequestResponse.Result(
                            id = authSigningPayload.authId.toLong(),
                            signature = Wallet.Model.Cacao.Signature(
                                t = cacaoSignature.t,
                                s = cacaoSignature.s,
                                m = cacaoSignature.m
                            ),
                            issuer = authSigningPayload.issuer
                        ),
                        onSuccess = {
                            Timber.d("Auth request response: $it")
                        },
                        onError = { error ->
                            Timber.e("Auth request response error: $error")
                        }
                    )
                } catch (e: Exception) {
                    Timber.e("Auth request response error: $e")
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun buildAuthSigningPayload(authId: String): Flow<WalletConnectAuthSigningPayload> =
        withContext(Dispatchers.IO) {
            // Retrieve the authentication request with the given ID
            getAuthRequest(authId)?.let { authRequest ->
                // Retrieve the supported EVMs networks as a flow
                getSupportedEvmNetworks().asFlow()
                    .map { coinNetworks ->
                        // Find the EVM network with the matching chain ID from the authentication request
                        coinNetworks.first {
                            it.chainId == authRequest.payloadParams.chainId.substringAfter(":").toInt()
                        }.networkTicker
                    }.flatMapLatest { networkTicker ->
                        // Retrieve the DeFi account associated with the network ticker
                        getDeFiAccountFromTicker(networkTicker)
                    }.flatMapLatest { accountList ->
                        // Retrieve the receive address for the default account in the account list
                        accountList.first { it.isDefault }.receiveAddress.asFlow()
                    }.map { receiveAddress ->
                        val issuer = "$ISS_DID_PREFIX${authRequest.payloadParams.chainId}:${receiveAddress.address}"
                        val message = authRequest.getAuthMessage(issuer)

                        WalletConnectAuthSigningPayload(
                            authId = authId,
                            authMessage = message,
                            issuer = issuer,
                            domain = authRequest.payloadParams.domain
                        )
                    }
            } ?: throw Exception("Auth request not found")
        }

    override suspend fun rejectAuthRequest(authId: String) {
        withContext(Dispatchers.IO) {
            try {
                Web3Wallet.respondAuthRequest(
                    params = Wallet.Params.AuthRequestResponse.Error(
                        id = authId.toLong(),
                        code = 500,
                        message = "User rejected the request"
                    ),
                    onSuccess = {
                        Timber.d("Auth request response: $it")
                    },
                    onError = { error ->
                        Timber.e("Auth request response error: $error")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Auth request response error: $e")
            }
        }
    }

    override fun Wallet.Model.PendingAuthRequest.getAuthMessage(issuer: String) =
        Web3Wallet.formatMessage(
            Wallet.Params.FormatMessage(this.payloadParams, issuer)
        ) ?: throw Exception("Error formatting message")

    override fun validateURI(uri: String): Boolean {
        val regex = Regex("^wc:(?:/{0,2})([0-9a-fA-F]+)@2\\?(.*)")
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

    private fun getDeFiAccountFromTicker(networkTicker: String) =
        coincore.activeWalletsInMode(
            walletMode = WalletMode.NON_CUSTODIAL,
        ).map {
            it.accounts
        }.map {
            it.filterIsInstance<CryptoAccount>()
        }.map { accounts ->
            accounts.filter { account ->
                account.currency.networkTicker == networkTicker
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
        scope.launch {
            _walletEvents.emit(authRequest)
        }
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
