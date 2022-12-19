package com.blockchain.walletconnect.ui.dapps

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectServiceAPI
import com.blockchain.walletconnect.domain.WalletConnectSession
import io.reactivex.rxjava3.core.Scheduler
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
) : MviModel<DappsListState, DappsListIntent>(
    initialState = DappsListState(),
    uiScheduler = uiSchedulers,
    environmentConfig = environmentConfig,
    remoteLogger = remoteLogger
) {
    override fun performAction(previousState: DappsListState, intent: DappsListIntent): Disposable? {
        return when (intent) {
            DappsListIntent.LoadDapps -> sessionsRepository.retrieve()
                .onErrorReturn { emptyList() }
                .subscribeBy { sessions ->
                    process(DappsListIntent.DappsLoaded(sessions))
                }
            is DappsListIntent.Disconnect -> walletConnectServiceAPI.disconnect(intent.session)
                .subscribeBy(
                    onComplete = {
                        process(DappsListIntent.LoadDapps)
                    }, onError = {
                        process(DappsListIntent.LoadDapps)
                        Timber.e("Failed to disconnect $it")
                    }
                )
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
