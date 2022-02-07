package com.blockchain.walletconnect.data

import com.blockchain.coincore.TxResult
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionEvent
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.IllegalArgumentException
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class WalletConnectService(
    private val walletConnectAccountProvider: WalletConnectAddressProvider,
    private val sessionRepository: SessionRepository,
    private val featureFlag: IntegratedFeatureFlag,
    private val ethRequestSign: EthRequestSign,
    private val ethSendTransactionRequest: EthSendTransactionRequest,
    lifecycleObservable: LifecycleObservable,
    private val client: OkHttpClient
) : WalletConnectServiceAPI, WalletConnectUrlValidator, WebSocketListener() {

    private val wcClients: HashMap<String, WCClient> = hashMapOf()
    private val compositeDisposable = CompositeDisposable()

    private val _sessionEvents = PublishSubject.create<WalletConnectSessionEvent>()
    override val sessionEvents: Observable<WalletConnectSessionEvent>
        get() = _sessionEvents

    private val _walletConnectUserEvents = PublishSubject.create<WalletConnectUserEvent>()
    override val userEvents: Observable<WalletConnectUserEvent>
        get() = _walletConnectUserEvents

    init {
        compositeDisposable += lifecycleObservable.onStateUpdated.subscribe {
            // TODO connect and disconnect
        }
    }

    override fun init() {
        compositeDisposable += featureFlag.enabled.flatMap { enabled ->
            if (enabled)
                sessionRepository.retrieve()
            else sessionRepository.removeAll().toSingle { emptyList() }
        }.zipWith(walletConnectAccountProvider.address()).subscribe { (sessions, _) ->
            sessions.forEach { session ->
                val wcClient = WCClient(httpClient = client)
                val wcSession = WCSession.from(session.url) ?: throw IllegalArgumentException(
                    "Not a valid wallet connect url ${session.url}"
                )
                wcClient.connect(
                    session = wcSession,
                    peerId = session.walletInfo.clientId,
                    remotePeerId = session.dAppInfo.peerId,
                    peerMeta = session.dAppInfo.toWCPeerMeta()
                )
                wcClient.addSignEthHandler(session)
                wcClient.addEthSendTransactionHandler(session)
            }
        }
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
            wcClient.configureFailureAndDisconnection(session)
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
        wcClients[session.url]?.addSignEthHandler(session)
        wcClients[session.url]?.addEthSendTransactionHandler(session)
    }

    private fun onSessionConnectFailedOrDisconnected(wcSession: WCSession) {
        wcClients.remove(wcSession.toUri())
        compositeDisposable += sessionRepository.retrieve().flatMapCompletable { sessions ->
            val session = sessions.firstOrNull { it.url == wcSession.toUri() }
            if (session == null) {
                Completable.complete()
            } else {
                sessionRepository.remove(session)
            }
        }.emptySubscribe()
    }

    override fun acceptConnection(session: WalletConnectSession): Completable =
        walletConnectAccountProvider.address().map { address ->
            wcClients[session.url]?.approveSession(
                listOf(address),
                WalletConnectSession.DEFAULT_WALLET_CONNECT_CHAIN_ID
            )
        }.map { approved ->
            if (approved == true) {
                onSessionApproved(session)
            } else {
                onFailToConnect(session)
            }
        }.ignoreElement()

    private fun onFailToConnect(session: WalletConnectSession) {
        _sessionEvents.onNext(WalletConnectSessionEvent.FailToConnect(session))
        wcClients.remove(session.url)
    }

    override fun denyConnection(session: WalletConnectSession): Completable = Completable.fromCallable {
        wcClients[session.url]?.rejectSession()
        wcClients[session.url]?.disconnect()
    }.onErrorComplete().doOnComplete {
        onSessionRejected(session)
    }

    private fun onSessionRejected(session: WalletConnectSession) {
        _sessionEvents.onNext(WalletConnectSessionEvent.DidReject(session))
        wcClients.remove(session.url)
    }

    override fun disconnect(session: WalletConnectSession): Completable =
        Completable.fromCallable {
            wcClients[session.url]?.disconnect()
        }.onErrorComplete().doOnComplete {
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

    private fun WCClient.configureFailureAndDisconnection(wcSession: WCSession) {
        onFailure = {
            onSessionConnectFailedOrDisconnected(wcSession)
        }
        onDisconnect = { _, _ ->
            onSessionConnectFailedOrDisconnected(wcSession)
        }
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
                        }
                    } ?: Completable.complete()
                },
                onTxCancelled = {
                    Completable.fromCallable {
                        this.rejectRequest(id)
                    }
                }
            ).subscribeBy(onSuccess = {
                _walletConnectUserEvents.onNext(it)
            }, onError = {})
        }
    }

    private fun WCClient.addEthSendTransactionHandler(session: WalletConnectSession) {
        onEthSendTransaction = ethTransactionHandler(EthereumSendTransactionTarget.Method.SEND, session)

        onEthSignTransaction = ethTransactionHandler(EthereumSendTransactionTarget.Method.SIGN, session)
    }

    private fun WCClient.ethTransactionHandler(
        method: EthereumSendTransactionTarget.Method,
        session: WalletConnectSession
    ): (Long, WCEthereumTransaction) -> Unit = { id, message ->
        compositeDisposable += ethSendTransactionRequest.onSendTransaction(
            transaction = message,
            session = session,
            method = method,
            onTxCompleted = { txResult ->
                (txResult as? TxResult.HashedTxResult)?.let { result ->
                    Completable.fromCallable {
                        this.approveRequest(id, result.txId)
                    }
                } ?: Completable.complete()
            },
            onTxCancelled = {
                Completable.fromCallable {
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
