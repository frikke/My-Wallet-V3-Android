package com.dex.presentation

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.dex.domain.DexAccount
import com.dex.domain.DexAccountsService
import com.dex.domain.DexNetworkService
import com.dex.domain.DexTransactionProcessor
import com.dex.presentation.uierrors.DexUiError
import com.dex.presentation.uierrors.uiErrors
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DexSourceAccountViewModel(
    private val dexService: DexAccountsService,
    private val transactionProcessor: DexTransactionProcessor,
    private val dexNetworkService: DexNetworkService
) : MviViewModel<
    SourceAccountIntent,
    SourceAccountSelectionViewState,
    SourceAccountModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = SourceAccountModelState(
        accounts = emptyList(),
        txInProgressStatus = InProgressTxStatus.None,
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun SourceAccountModelState.reduce() = SourceAccountSelectionViewState(
        accounts = this.accounts.filter { account ->
            searchFilter.isEmpty() ||
                account.currency.networkTicker.contains(searchFilter, true) ||
                account.currency.displayTicker.contains(searchFilter, true) ||
                account.currency.name.contains(searchFilter, true)
        }.sortedByDescending {
            it.fiatBalance
        },
        inProgressTxStatus = txInProgressStatus
    )

    override suspend fun handleIntent(modelState: SourceAccountModelState, intent: SourceAccountIntent) {
        when (intent) {
            SourceAccountIntent.LoadSourceAccounts -> {
                viewModelScope.launch {
                    dexService.sourceAccounts(chainId = dexNetworkService.selectedChainId())
                        .collectLatest { dexAccounts ->
                            updateState {
                                copy(
                                    accounts = dexAccounts
                                )
                            }
                        }
                }

                viewModelScope.launch {
                    transactionProcessor.transaction.map { it.uiErrors() }.collectLatest { errors ->
                        updateState {
                            copy(
                                txInProgressStatus = errors
                                    .filterIsInstance<DexUiError.TransactionInProgressError>()
                                    .firstOrNull()
                                    .takeIf { this.txInProgressDismissed.not() }?.let {
                                        InProgressTxStatus.PendingTx(it.coinNetwork)
                                    } ?: InProgressTxStatus.None
                            )
                        }
                    }
                }
            }

            is SourceAccountIntent.OnAccountSelected -> {
                transactionProcessor.updateSourceAccount(intent.account)
            }

            is SourceAccountIntent.Search -> {
                updateState {
                    copy(
                        searchFilter = intent.query
                    )
                }
            }

            SourceAccountIntent.WarningDismissed -> updateState {
                copy(
                    txInProgressDismissed = true,
                    txInProgressStatus = InProgressTxStatus.None
                )
            }
        }
    }
}

sealed class SourceAccountIntent : Intent<SourceAccountModelState> {
    object LoadSourceAccounts : SourceAccountIntent()
    object WarningDismissed : SourceAccountIntent()
    data class OnAccountSelected(val account: DexAccount) : SourceAccountIntent()
    data class Search(val query: String) : SourceAccountIntent()
}

data class SourceAccountSelectionViewState(
    val accounts: List<DexAccount>,
    val inProgressTxStatus: InProgressTxStatus,
) : ViewState

data class SourceAccountModelState(
    val accounts: List<DexAccount> = emptyList(),
    val searchFilter: String = "",
    val txInProgressStatus: InProgressTxStatus,
    val txInProgressDismissed: Boolean = false
) : ModelState

sealed class InProgressTxStatus {
    object None : InProgressTxStatus()
    data class PendingTx(val coinNetwork: CoinNetwork) : InProgressTxStatus()
}
