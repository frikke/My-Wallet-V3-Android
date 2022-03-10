package com.blockchain.blockchaincard.ui

import com.blockchain.blockchaincard.data.BcCardDataRepository
import com.blockchain.blockchaincard.data.BcCardService
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable

data class OrderCardState(
    val tbd: String = ""
) : MviState

class OrderCardModel(
    uiSchedulers: Scheduler,
    enviromentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val bcCardDataRepository: BcCardDataRepository,
    private val bcCardService: BcCardService
) : MviModel<OrderCardState, OrderCardIntent>(
    initialState = OrderCardState(),
    uiScheduler = uiSchedulers,
    environmentConfig = enviromentConfig,
    crashLogger = crashLogger
) {
    override fun performAction(previousState: OrderCardState, intent: OrderCardIntent): Disposable? {
        TODO("Not yet implemented")
    }
}


sealed class OrderCardIntent : MviIntent<OrderCardState> {

}