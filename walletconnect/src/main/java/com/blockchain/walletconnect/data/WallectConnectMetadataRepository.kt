package com.blockchain.walletconnect.data

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletInfo
import com.blockchain.walletconnect.ui.networks.ETH_CHAIN_ID
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WalletConnectMetadataRepository(
    private val metadataRepository: MetadataRepository,
    private val walletConnectSessionsStorage: WalletConnectSessionsStorage
) : SessionRepository {

    override fun contains(session: WalletConnectSession): Single<Boolean> = loadSessions().map {
        it.contains(session)
    }

    private val jsonBuilder = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun loadSessions(): Single<List<WalletConnectSession>> =
        walletConnectSessionsStorage.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).asSingle().map { walletConnectMetadata ->
            walletConnectMetadata.sessions?.let { sessions ->
                sessions.v1.map { dapp ->
                    WalletConnectSession(
                        url = dapp.url,
                        dAppInfo = DAppInfo(
                            peerId = dapp.dAppInfo.peerId,
                            peerMeta = dapp.dAppInfo.peerMeta.toClientMeta(),
                            chainId = dapp.dAppInfo.chainId ?: ETH_CHAIN_ID
                        ),
                        walletInfo = WalletInfo(
                            clientId = dapp.walletInfo.clientId,
                            sourcePlatform = dapp.walletInfo.sourcePlatform
                        )
                    )
                }
            } ?: emptyList()
        }

    private fun updateRemoteSessions(sessions: List<WalletConnectSession>): Completable =
        metadataRepository.saveRawValue(sessions.toJsonMetadata(), MetadataEntry.WALLET_CONNECT_METADATA).doOnComplete {
            walletConnectSessionsStorage.markAsStale()
        }

    override fun store(session: WalletConnectSession): Completable =
        loadSessions().flatMapCompletable { sessions ->
            if (sessions.contains(session)) {
                Completable.complete()
            } else {
                val newSessions = sessions + session
                updateRemoteSessions(newSessions)
            }
        }

    override fun remove(session: WalletConnectSession): Completable =
        loadSessions().flatMapCompletable { storedSessions ->
            val newSessions = storedSessions.toMutableList().apply {
                remove(session)
            }
            updateRemoteSessions(newSessions)
        }

    override fun retrieve(): Single<List<WalletConnectSession>> = loadSessions()

    override fun removeAll(): Completable = updateRemoteSessions(emptyList())

    private fun List<WalletConnectSession>.toJsonMetadata(): String {
        val metadataConnectSessions = WalletConnectMetadata(
            sessions = WalletConnectSessions(
                v1 = this.map { walletConnectSession ->
                    WalletConnectDapps(
                        url = walletConnectSession.url,
                        dAppInfo = DappInfo(
                            chainId = walletConnectSession.dAppInfo.chainId,
                            peerId = walletConnectSession.dAppInfo.peerId,
                            peerMeta = walletConnectSession.dAppInfo.peerMeta.toPeerMeta()
                        ),
                        walletInfo = walletConnectSession.walletInfo.toWalletMetadaInfo()
                    )
                }
            )
        )
        return jsonBuilder.encodeToString(metadataConnectSessions)
    }
}

private fun WalletInfo.toWalletMetadaInfo(): WalletInfoMetadata =
    WalletInfoMetadata(
        sourcePlatform = sourcePlatform,
        clientId = clientId
    )

private fun PeerMeta.toClientMeta(): ClientMeta =
    ClientMeta(
        description = description,
        icons = icons,
        url = url,
        name = name
    )

private fun ClientMeta.toPeerMeta(): PeerMeta =
    PeerMeta(
        description = description,
        icons = icons,
        url = url,
        name = name
    )
