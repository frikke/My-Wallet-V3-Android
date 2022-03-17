package com.blockchain.blockchaincard.ui

import com.blockchain.blockchaincard.data.BcCardDataRepository
import com.blockchain.blockchaincard.data.BcCardService
import com.blockchain.blockchaincard.ui.navigation.BlockchainCardNavigator
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable

data class BlockchainCardState(
    val cardState: CardState = CardState.UNKNOWN
) : MviState

class BlockchainCardModel(
    uiSchedulers: Scheduler,
    enviromentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val bcCardDataRepository: BcCardDataRepository,
    private val bcCardService: BcCardService,
    val navigator: BlockchainCardNavigator
) : MviModel<BlockchainCardState, BlockchainCardIntent>(
    initialState = BlockchainCardState(),
    uiScheduler = uiSchedulers,
    environmentConfig = enviromentConfig,
    crashLogger = crashLogger
) {
    override fun performAction(previousState: BlockchainCardState, intent: BlockchainCardIntent): Disposable? {
        return when (intent) {
            BlockchainCardIntent.OrderCard -> null
            BlockchainCardIntent.LinkCard -> null
            BlockchainCardIntent.ManageCard -> null
            is BlockchainCardIntent.UpdateCardState -> null
        }
    }

    private fun navigate() {
        navigator.navigateTo("order_or_link_card")
    }
}


sealed class BlockchainCardIntent : MviIntent<BlockchainCardState> {
    object OrderCard : BlockchainCardIntent() {
        override fun reduce(oldState: BlockchainCardState): BlockchainCardState = oldState
    }

    object LinkCard : BlockchainCardIntent() {
        override fun reduce(oldState: BlockchainCardState): BlockchainCardState = oldState
    }

    object ManageCard : BlockchainCardIntent() {
        override fun reduce(oldState: BlockchainCardState): BlockchainCardState = oldState
    }

    data class UpdateCardState(private val cardState: CardState) : BlockchainCardIntent() {
        override fun reduce(oldState: BlockchainCardState): BlockchainCardState = oldState.copy(
            cardState = cardState
        )
    }
}

enum class CardState {
    NOT_ORDERED,
    CREATED,
    UNKNOWN
}