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
import kotlinx.coroutines.flow.collectLatest
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
        accounts = emptyList()
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: SourceAccountModelState): SourceAccountSelectionViewState {
        return with(state) {
            SourceAccountSelectionViewState(
                accounts = this.accounts.filter { account ->
                    searchFilter.isEmpty() ||
                        account.currency.networkTicker.contains(searchFilter, true) ||
                        account.currency.displayTicker.contains(searchFilter, true) ||
                        account.currency.name.contains(searchFilter, true)
                }.sortedByDescending {
                    it.fiatBalance
                }
            )
        }
    }

    override suspend fun handleIntent(modelState: SourceAccountModelState, intent: SourceAccountIntent) {
        when (intent) {
            SourceAccountIntent.LoadSourceAccounts -> {
                viewModelScope.launch {
                    dexService.sourceAccounts(chainId = dexNetworkService.selectedChainId())
                        .collectLatest { dexAccounts ->
                            updateState {
                                it.copy(
                                    accounts = dexAccounts
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
                    it.copy(
                        searchFilter = intent.query
                    )
                }
            }
        }
    }
}

sealed class SourceAccountIntent : Intent<SourceAccountModelState> {
    object LoadSourceAccounts : SourceAccountIntent()
    class OnAccountSelected(val account: DexAccount) : SourceAccountIntent()
    class Search(val query: String) : SourceAccountIntent()
}

data class SourceAccountSelectionViewState(
    val accounts: List<DexAccount> = emptyList()
) : ViewState

data class SourceAccountModelState(
    val accounts: List<DexAccount> = emptyList(),
    val searchFilter: String = ""
) : ModelState
