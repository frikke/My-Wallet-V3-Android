package com.blockchain.walletconnect.ui.dapps

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import com.blockchain.walletconnect.domain.WalletInfo
import com.blockchain.walletconnect.ui.networks.ETH_CHAIN_ID
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

data class DappsListState(
    val connectedSessions: List<WalletConnectSession> = emptyList()
) : MviState

class DappsListModel(
    uiSchedulers: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger,
    private val sessionsRepository: SessionRepository,
    private val walletConnectServiceAPI: WalletConnectServiceAPI,
    private val walletConnectV2Service: WalletConnectV2Service,
    private val walletConnectV2FeatureFlag: FeatureFlag
) : MviModel<DappsListState, DappsListIntent>(
    initialState = DappsListState(),
    uiScheduler = uiSchedulers,
    environmentConfig = environmentConfig,
    remoteLogger = remoteLogger
) {
    override fun performAction(previousState: DappsListState, intent: DappsListIntent): Disposable? {
        return when (intent) {
            DappsListIntent.LoadDapps -> {
                Single.zip(
                    sessionsRepository.retrieve().onErrorReturn { emptyList() },
                    walletConnectV2FeatureFlag.enabled
                ) { sessionsV1, isV2Enabled ->
                    val sessionsV2 = if (isV2Enabled)
                        walletConnectV2Service.getSessions().map { session ->
                            WalletConnectSession(
                                url = session.metaData?.redirect.orEmpty(),
                                dAppInfo = DAppInfo(
                                    peerId = "", // This can be empty
                                    peerMeta = ClientMeta(
                                        description = session.metaData?.description.orEmpty(),
                                        url = session.metaData?.url.orEmpty(),
                                        icons = session.metaData?.icons.orEmpty(),
                                        name = session.metaData?.name.orEmpty()
                                    ),
                                    chainId = ETH_CHAIN_ID,
                                ),
                                walletInfo = WalletInfo(
                                    clientId = session.topic,
                                    sourcePlatform = "Android"
                                ),
                                isV2 = true
                            )
                        }
                    else emptyList()

                    sessionsV1 + sessionsV2
                }.subscribeBy { sessions ->
                    process(DappsListIntent.DappsLoaded(sessions))
                }
            }
            is DappsListIntent.Disconnect -> {
                if (intent.session.isV2) {
                    walletConnectV2Service.disconnectSession(intent.session.walletInfo.clientId)
                    process(DappsListIntent.LoadDapps)
                    null
                } else {
                    walletConnectServiceAPI.disconnect(intent.session)
                        .subscribeBy(
                            onComplete = {
                                process(DappsListIntent.LoadDapps)
                            }, onError = {
                                process(DappsListIntent.LoadDapps)
                                Timber.e("Failed to disconnect $it")
                            }
                        )
                }
            }
            is DappsListIntent.DappsLoaded -> null
        }
    }
}

sealed class DappsListIntent : MviIntent<DappsListState> {
    object LoadDapps : DappsListIntent() {
        override fun reduce(oldState: DappsListState): DappsListState = oldState
    }

    class Disconnect(val session: WalletConnectSession) : DappsListIntent() {
        override fun reduce(oldState: DappsListState): DappsListState = oldState
    }

    data class DappsLoaded(private val dapps: List<WalletConnectSession>) : DappsListIntent() {
        override fun reduce(oldState: DappsListState): DappsListState = oldState.copy(
            connectedSessions = dapps
        )
    }
}
