package com.blockchain.walletconnect.ui.dapps

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import com.blockchain.walletconnect.domain.SessionRepository
import com.blockchain.walletconnect.domain.WalletConnectSession
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

data class DappsListState(val connectedSessions: List<WalletConnectSession> = emptyList()) : MviState

class DappsListModel(
    uiSchedulers: Scheduler,
    enviromentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val sessionsRepository: SessionRepository
) : MviModel<DappsListState, DappsListIntent>(
    initialState = DappsListState(),
    uiScheduler = uiSchedulers,
    environmentConfig = enviromentConfig,
    crashLogger = crashLogger
) {
    override fun performAction(previousState: DappsListState, intent: DappsListIntent): Disposable? {
        return when (intent) {
            DappsListIntent.LoadDapps -> sessionsRepository.retrieve().subscribeBy { sessions ->
                println("Fetcheddd XXXXX $sessions")
                process(DappsListIntent.DappsLoaded(sessions))
            }
            is DappsListIntent.DappsLoaded -> null
        }
    }
}

sealed class DappsListIntent : MviIntent<DappsListState> {
    object LoadDapps : DappsListIntent() {
        override fun reduce(oldState: DappsListState): DappsListState = oldState
    }

    data class DappsLoaded(private val dapps: List<WalletConnectSession>) : DappsListIntent() {
        override fun reduce(oldState: DappsListState): DappsListState = oldState.copy(
            connectedSessions = dapps
        )
    }
}
