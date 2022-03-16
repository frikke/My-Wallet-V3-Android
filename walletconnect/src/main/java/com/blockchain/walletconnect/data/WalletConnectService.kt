package com.blockchain.walletconnect.data

import com.blockchain.coincore.TxResult
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.extensions.exhaustive
import com.blockchain.lifecycle.AppState
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectAnalytics
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionEvent
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.blockchain.walletconnect.domain.toAnalyticsMethod
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.WS_CLOSE_NORMAL
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.then

class WalletConnectService(
    private val walletConnectAccountProvider: WalletConnectAddressProvider,
    private val sessionRepository: SessionRepository,
    private val featureFlag: IntegratedFeatureFlag,
    private val ethRequestSign: EthRequestSign,
    private val ethSendTransactionRequest: EthSendTransactionRequest,
    private val lifecycleObservable: LifecycleObservable,
    private val analytics: Analytics,
    private val client: OkHttpClient
) : WalletConnectServiceAPI, WalletConnectUrlValidator, WebSocketListener() {

    private val wcClients: HashMap<String, WCClient> = hashMapOf()
    private val connectedSessions = mutableListOf<WalletConnectSession>()
    private val compositeDisposable = CompositeDisposable()

    private val _sessionEvents = PublishSubject.create<WalletConnectSessionEvent>()
    override val sessionEvents: Observable<WalletConnectSessionEvent>
        get() = _sessionEvents

    private val _walletConnectUserEvents = PublishSubject.create<WalletConnectUserEvent>()
    override val userEvents: Observable<WalletConnectUserEvent>
        get() = _walletConnectUserEvents

    private fun reconnectToPreviouslyApprovedSessions() {
        connectedSessions.forEach {
            it.connect()
        }
    }

    private fun disconnectAllClientsTemporarily() {
        wcClients.forEach { (_, client) ->
            client.onDisconnect = { _, _ -> }
            client.disconnect()
        }
        wcClients.clear()
    }

    override fun init() {
        compositeDisposable += featureFlag.enabled.flatMap { enabled ->
            if (enabled)
                sessionRepository.retrieve()
            else Single.just(emptyList())
        }.zipWith(walletConnectAccountProvider.address()).subscribe { (sessions, _) ->
            sessions.forEach { session ->
                session.connect()
                connectedSessions.add(session)
            }
        }

        compositeDisposable += lifecycleObservable.onStateUpdated.subscribe { state ->
            when (state) {
                null -> {
                } // warning removal
                AppState.BACKGROUNDED -> disconnectAllClientsTemporarily() // Close sockets when app gets backgrounded.
                AppState.FOREGROUNDED -> reconnectToPreviouslyApprovedSessions()
            }.exhaustive
        }
    }

    private fun WalletConnectSession.connect() {
        val wcClient = WCClient(httpClient = client)
        val wcSession = WCSession.from(url) ?: throw IllegalArgumentException(
            "Not a valid wallet connect url $url"
        )

        wcClient.connect(
            session = wcSession,
            peerId = walletInfo.clientId,
            remotePeerId = dAppInfo.peerId,
            peerMeta = dAppInfo.toWCPeerMeta()
        )
        wcClients[url] = wcClient
        wcClient.configureDisconnect(this)
        wcClient.addSignEthHandler(this)
        wcClient.addEthSendTransactionHandler(this)
    }

    override fun attemptToConnect(url: String): Completable {
        return Completable.fromCallable {
            val wcClient = WCClient(httpClient = client)
            val session = WCSession.from(url) ?: throw IllegalArgumentException(
                "Not a valid wallet connect url $url"
            )
            val peerId = UUID.randomUUID().toString()

            wcClient.onSessionRequest = { _, peerMeta ->
                onNewSessionRequested(session, peerMeta, wcClient.remotePeerId.orEmpty(), peerId)
            }

            wcClients[session.toUri()] = wcClient
            wcClient.connect(
                session = session,
                peerId = peerId,
                peerMeta = DEFAULT_PEER_META
            )
        }
    }

    private fun onSessionApproved(session: WalletConnectSession) {
        _sessionEvents.onNext(WalletConnectSessionEvent.DidConnect(session))
        compositeDisposable += sessionRepository.store(session).onErrorComplete().emptySubscribe()
        connectedSessions.add(session)
        wcClients[session.url]?.addSignEthHandler(session)
        wcClients[session.url]?.addEthSendTransactionHandler(session)
        wcClients[session.url]?.configureDisconnect(session)
    }

    override fun acceptConnection(session: WalletConnectSession): Completable =
        walletConnectAccountProvider.address().map { address ->
            wcClients[session.url]?.approveSession(
                listOf(address),
                WalletConnectSession.DEFAULT_WALLET_CONNECT_CHAIN_ID
            ) ?: throw IllegalStateException("No connected client found")
        }.map { approved ->
            if (approved == true) {
                onSessionApproved(session)
            } else {
                onFailToConnect(session)
            }
        }.ignoreElement()

    private fun onFailToConnect(session: WalletConnectSession) {
        wcClients[session.url]?.disconnect()
        _sessionEvents.onNext(WalletConnectSessionEvent.FailToConnect(session))
        wcClients.remove(session.url)
    }

    override fun denyConnection(session: WalletConnectSession): Completable = Completable.fromCallable {
        wcClients[session.url]?.rejectSession()
        wcClients[session.url]?.disconnect()
    }.onErrorComplete().doOnComplete {
        onSessionRejected(session)
    }

    override fun clear() {
        compositeDisposable.clear()
        connectedSessions.clear()
        disconnectAndClear()
    }

    private fun disconnectAndClear() {
        wcClients.forEach { (_, client) ->
            client.disconnect()
        }
        wcClients.clear()
    }

    private fun onSessionRejected(session: WalletConnectSession) {
        _sessionEvents.onNext(WalletConnectSessionEvent.DidReject(session))
        wcClients.remove(session.url)
    }

    override fun disconnect(session: WalletConnectSession): Completable =
        Completable.fromCallable {
            wcClients[session.url]?.killSession()
        }.then {
            sessionRepository.remove(session)
        }.onErrorComplete().doOnComplete {
            wcClients[session.url]?.disconnect()
            wcClients.remove(session.url)
            connectedSessions.remove(session)
            _sessionEvents.onNext(WalletConnectSessionEvent.DidDisconnect(session))
        }

    private fun onNewSessionRequested(session: WCSession, peerMeta: WCPeerMeta, remotePeerId: String, peerId: String) {
        _sessionEvents.onNext(
            WalletConnectSessionEvent.ReadyForApproval(
                WalletConnectSession.fromWCSession(
                    wcSession = session,
                    peerMeta = peerMeta,
                    remotePeerId = remotePeerId,
                    peerId = peerId,
                )
            )
        )
    }

    companion object {
        private val DEFAULT_PEER_META = WCPeerMeta(
            name = "Blockchain.com",
            url = "https://blockchain.com",
            icons = listOf("https://www.blockchain.com/static/apple-touch-icon.png")
        )
    }

    private fun WCClient.addSignEthHandler(session: WalletConnectSession) {
        onEthSign = { id, message ->
            compositeDisposable += ethRequestSign.onEthSign(
                message = message,
                session = session,
                onTxCompleted = { txResult ->
                    (txResult as? TxResult.HashedTxResult)?.let { result ->
                        Completable.fromCallable {
                            this.approveRequest(id, result.txId)
                            analytics.logEvent(
                                WalletConnectAnalytics.DappRequestActioned(
                                    method = message.toAnalyticsMethod(),
                                    action = WalletConnectAnalytics.DappConnectionAction.CONFIRM,
                                    appName = session.dAppInfo.peerMeta.name
                                )
                            )
                        }
                    } ?: Completable.complete()
                }
            ) {
                Completable.fromCallable {
                    analytics.logEvent(
                        WalletConnectAnalytics.DappRequestActioned(
                            method = message.toAnalyticsMethod(),
                            action = WalletConnectAnalytics.DappConnectionAction.CANCEL,
                            appName = session.dAppInfo.peerMeta.name
                        )
                    )
                    this.rejectRequest(id)
                }
            }.subscribeBy(onSuccess = {
                _walletConnectUserEvents.onNext(it)
            }, onError = {})
        }
    }

    private fun WCClient.configureDisconnect(session: WalletConnectSession) {
        onDisconnect = { code, _ ->
            if (code == WS_CLOSE_NORMAL) {
                removeSessionFromRepo(session)
                killSession()
                onDisconnect = { _, _ -> }
            }
        }
    }

    private fun removeSessionFromRepo(wcSession: WalletConnectSession) {
        wcClients.remove(wcSession.url)
        compositeDisposable += sessionRepository.retrieve().flatMapCompletable { sessions ->
            val session = sessions.firstOrNull { it.url == wcSession.url }
            if (session == null) {
                Completable.complete()
            } else {
                sessionRepository.remove(session)
            }
        }.emptySubscribe()
    }

    private fun WCClient.addEthSendTransactionHandler(session: WalletConnectSession) {
        onEthSendTransaction = ethTransactionHandler(EthereumSendTransactionTarget.Method.SEND, session)

        onEthSignTransaction = ethTransactionHandler(EthereumSendTransactionTarget.Method.SIGN, session)
    }

    private fun WCClient.ethTransactionHandler(
        method: EthereumSendTransactionTarget.Method,
        session: WalletConnectSession
    ): (Long, WCEthereumTransaction) -> Unit = { id, transaction ->
        compositeDisposable += ethSendTransactionRequest.onSendTransaction(
            transaction = transaction,
            session = session,
            method = method,
            onTxCompleted = { txResult ->
                (txResult as? TxResult.HashedTxResult)?.let { result ->
                    Completable.fromCallable {
                        this.approveRequest(id, result.txId)

                        analytics.logEvent(
                            WalletConnectAnalytics.DappRequestActioned(
                                method = method.toAnalyticsMethod(),
                                action = WalletConnectAnalytics.DappConnectionAction.CONFIRM,
                                appName = session.dAppInfo.peerMeta.name
                            )
                        )
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                Completable.fromCallable {
                    analytics.logEvent(
                        WalletConnectAnalytics.DappRequestActioned(
                            method = method.toAnalyticsMethod(),
                            action = WalletConnectAnalytics.DappConnectionAction.CANCEL,
                            appName = session.dAppInfo.peerMeta.name
                        )
                    )
                    this.rejectRequest(id)
                }
            }
        ).subscribeBy(onSuccess = {
            _walletConnectUserEvents.onNext(it)
        }, onError = {})
    }

    override fun isUrlValid(url: String): Boolean =
        WCSession.from(url) != null
}

private fun DAppInfo.toWCPeerMeta(): WCPeerMeta =
    WCPeerMeta(
        name = peerMeta.name,
        description = peerMeta.description,
        icons = peerMeta.icons,
        url = peerMeta.url
    )
