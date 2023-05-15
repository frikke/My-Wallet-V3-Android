package com.dex.presentation.inprogress

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.outcome.Outcome
import com.dex.domain.DexTransaction
import com.dex.domain.DexTransactionProcessor
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DexInProgressTxViewModel(private val txProcessor: DexTransactionProcessor) : MviViewModel<
    InProgressIntent,
    InProgressViewState,
    InProgressModelState,
    InProgressNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = InProgressModelState(
        transaction = null,
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: InProgressModelState): InProgressViewState {
        return state.transaction?.let { transaction ->
            when (val result = transaction.txResult) {
                is Outcome.Success -> stateForSuccess(transaction, result.value)
                is Outcome.Failure -> InProgressViewState.Failure
                null -> throw IllegalStateException("Transaction should have been executed")
            }
        } ?: InProgressViewState.Loading
    }

    private fun stateForSuccess(transaction: DexTransaction, txId: String): InProgressViewState {
        val destinationCurrency =
            transaction.destinationAccount?.currency ?: throw IllegalStateException("Missing destination currency")
        return InProgressViewState.Success(
            sourceCurrency = transaction.sourceAccount.currency,
            destinationCurrency = destinationCurrency,
            txExplorerUrl = transaction.sourceAccount.currency.coinNetwork?.explorerUrl.plus("/$txId")
        )
    }

    override suspend fun handleIntent(modelState: InProgressModelState, intent: InProgressIntent) {
        when (intent) {
            InProgressIntent.LoadTransactionProgress -> {
                viewModelScope.launch {
                    val transaction = txProcessor.transaction.dropWhile { it.txResult == null }.first()
                    updateState {
                        it.copy(transaction = transaction)
                    }
                }
            }
        }
    }
}

data class InProgressModelState(
    val transaction: DexTransaction?
) : ModelState

object InProgressNavigationEvent : NavigationEvent
sealed class InProgressViewState : ViewState {
    data class Success(
        val sourceCurrency: AssetInfo,
        val destinationCurrency: AssetInfo,
        val txExplorerUrl: String
    ) : InProgressViewState()

    object Failure : InProgressViewState()

    object Loading : InProgressViewState()
}

sealed class InProgressIntent : Intent<InProgressModelState> {
    object LoadTransactionProgress : InProgressIntent()
}
