package com.blockchain.walletconnect.data

import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import java.lang.IllegalArgumentException
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

class WalletConnectService(
    private val walletConnectAccountProvider: WalletConnectAddressProvider,
    private val sessionRepository: SessionRepository,
    private val featureFlag: IntegratedFeatureFlag,
    private val client: OkHttpClient
) : WalletConnectServiceAPI, WebSocketListener() {

    private val wcClients: HashMap<String, WCClient> = hashMapOf()

    private val compositeDisposable = CompositeDisposable()

    override fun connectToApprovedSessions() {
        compositeDisposable += featureFlag.enabled.flatMap { enabled ->
            if (enabled)
                sessionRepository.retrieve()
            else Single.just(emptyList())
        }.subscribe { sessions ->
            sessions.forEach {
                val wcClient = WCClient(GsonBuilder(), client)
                val wcSession = WCSession.from(it.url) ?: throw IllegalArgumentException(
                    "Not a valid wallet connect url ${it.url}"
                )
                wcClient.configure(wcSession)
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

    override fun attemptToConnect(url: String) {
        val wcClient = WCClient(GsonBuilder(), client)
        val session = WCSession.from(url) ?: throw IllegalArgumentException(
            "Not a valida wallet connect url $url"
        )
        val peerId = UUID.randomUUID().toString()

        wcClient.onSessionRequest = { _, peerMeta ->
            onNewSessionRequested(session, peerMeta, peerId)
        }
        wcClient.configure(session)

        wcClient.connect(
            session = session,
            peerMeta = DEFAULT_PEER_META
        )
    }

    private fun onSessionApproved(session: WalletConnectSession) {
        // todo update Metadata
    }

    private fun onSessionDisconnected(session: WCSession) {
    }

    private fun onSessionConnectFailed(session: WCSession) {
    }

    override fun acceptConnection(session: WalletConnectSession) {
        compositeDisposable += walletConnectAccountProvider.address().map { address ->
            wcClients[session.url]?.approveSession(
                listOf(address),
                WalletConnectSession.DEFAULT_WALLET_CONNECT_CHAIN_ID
            )
        }.subscribe { approved ->
            if (approved == true) {
                onSessionApproved(session)
            }
        }
    }

    override fun denyConnection(session: WalletConnectSession) {
        wcClients[session.url]?.rejectSession()
    }

    fun disconnect(session: WalletConnectSession) {
        wcClients[session.url]?.disconnect()
    }

    private fun onNewSessionRequested(session: WCSession, peerMeta: WCPeerMeta, peerId: String) {
    }

    private fun WCClient.configure(wcSession: WCSession) {
        onFailure = {
            onSessionConnectFailed(wcSession)
        }
        onDisconnect = { _, _ ->
            onSessionDisconnected(wcSession)
        }
    }

    companion object {

        private val DEFAULT_PEER_META = WCPeerMeta(
            name = "Blockchain.com",
            url = "https://blockchain.com",
            icons = listOf("https://www.blockchain.com/static/apple-touch-icon.png")
        )
    }
}

private fun DAppInfo.toWCPeerMeta(): WCPeerMeta =
    WCPeerMeta(
        name = peerMeta.name,
        description = peerMeta.description,
        icons = peerMeta.icons,
        url = peerMeta.url
    )
