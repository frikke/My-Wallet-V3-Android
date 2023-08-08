package com.blockchain.walletconnect.ui.dapps

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectV2Service
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.rxSingle
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun performAction(previousState: DappsListState, intent: DappsListIntent): Disposable? {
        return when (intent) {
            DappsListIntent.LoadDapps -> {
                walletConnectV2FeatureFlag.enabled.flatMap { isV2Enabled ->
                    if (isV2Enabled) {
                        Single.zip(
                            sessionsRepository.retrieve().onErrorReturn { emptyList() },
                            rxSingle { walletConnectV2Service.getSessions() }
                        ) { sessionsV1, sessionsV2 ->
                            sessionsV1 + sessionsV2
                        }
                    } else {
                        sessionsRepository.retrieve().onErrorReturn { emptyList() }
                    }
                }.subscribeBy { sessions ->
                    process(DappsListIntent.DappsLoaded(sessions))
                }
            }
            is DappsListIntent.Disconnect -> {
                if (intent.session.isV2) {
                    scope.launch {
                        walletConnectV2Service.disconnectSession(intent.session.walletInfo.clientId)
                        process(DappsListIntent.LoadDapps)
                    }
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
