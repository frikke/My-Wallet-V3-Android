package com.blockchain.walletconnect.data

import com.blockchain.lifecycle.LifecycleObservable
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectSessionEvent
import com.blockchain.walletconnect.domain.WalletConnectUrlValidator
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.IllegalArgumentException
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber

class WalletConnectService(
    private val walletConnectAccountProvider: WalletConnectAddressProvider,
    private val sessionRepository: SessionRepository,
    private val featureFlag: IntegratedFeatureFlag,
    lifecycleObservable: LifecycleObservable,
    private val client: OkHttpClient
) : WalletConnectServiceAPI, WalletConnectUrlValidator, WebSocketListener() {

    private val wcClients: HashMap<String, WCClient> = hashMapOf()
    private val compositeDisposable = CompositeDisposable()

    private val _sessionEvents = PublishSubject.create<WalletConnectSessionEvent>()
    override val sessionEvents: Observable<WalletConnectSessionEvent>
        get() = _sessionEvents

    init {
        compositeDisposable += lifecycleObservable.onStateUpdated.subscribe {
            // TODO connect and disconnect
        }
    }

    override fun connectToApprovedSessions() {
        compositeDisposable += featureFlag.enabled.flatMap { enabled ->
            if (enabled)
                sessionRepository.retrieve()
            else sessionRepository.removeAll().toSingle { emptyList() }
        }.subscribe { sessions ->
            sessions.forEach {
                val wcClient = WCClient(httpClient = client)
                val wcSession = WCSession.from(it.url) ?: throw IllegalArgumentException(
                    "Not a valid wallet connect url ${it.url}"
                )
                wcClient.connect(
                    session = wcSession,
                    peerId = it.dAppInfo.peerId,
                    peerMeta = it.dAppInfo.toWCPeerMeta()
                )
                wcClient.addSocketListener(object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                        // Doing nothing for the time being
                        Timber.v("Session Connected ${it.dAppInfo.peerMeta.name}")
                    }
                })
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
                onNewSessionRequested(session, peerMeta, peerId)
            }
            wcClient.configureFailureAndDisconnetion(session)
            wcClients[session.toUri()] = wcClient
            wcClient.connect(
                session = session,
                peerMeta = DEFAULT_PEER_META
            )
        }
    }

    private fun onSessionApproved(session: WalletConnectSession) {
        _sessionEvents.onNext(WalletConnectSessionEvent.DidConnect(session))
        compositeDisposable += sessionRepository.store(session).onErrorComplete().emptySubscribe()
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

    private fun onNewSessionRequested(session: WCSession, peerMeta: WCPeerMeta, peerId: String) {
        _sessionEvents.onNext(
            WalletConnectSessionEvent.ReadyForApproval(
                WalletConnectSession.fromWCSession(
                    wcSession = session,
                    peerMeta = peerMeta,
                    peerId = peerId,
                )
            )
        )
    }

    private fun WCClient.configureFailureAndDisconnetion(wcSession: WCSession) {
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
