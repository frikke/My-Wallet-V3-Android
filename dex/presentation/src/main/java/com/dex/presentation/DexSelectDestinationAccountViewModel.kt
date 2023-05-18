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
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch

class DexSelectDestinationAccountViewModel(
    private val dexService: DexAccountsService,
    private val transactionProcessor: DexTransactionProcessor,
    private val dexNetworkService: DexNetworkService
) : MviViewModel<
    DestinationAccountIntent,
    DestinationAccountSelectionViewState,
    DestinationAccountModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = DestinationAccountModelState(
        accounts = emptyList()
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun DestinationAccountModelState.reduce() = DestinationAccountSelectionViewState(
        accounts = this.accounts.filter { account ->
            searchFilter.isEmpty() ||
                account.currency.networkTicker.contains(searchFilter, true) ||
                account.currency.displayTicker.contains(searchFilter, true) ||
                account.currency.name.contains(searchFilter, true)
        }.sortedWith(
            compareByDescending<DexAccount> { it.fiatBalance }
                .thenBy { it.currency.name }
        )
    )

    override suspend fun handleIntent(modelState: DestinationAccountModelState, intent: DestinationAccountIntent) {
        when (intent) {
            DestinationAccountIntent.LoadAccounts -> {
                viewModelScope.launch {
                    dexService.destinationAccounts(chainId = dexNetworkService.selectedChainId())
                        .zip(transactionProcessor.transaction) { accounts, tx ->
                            accounts.filter {
                                it.currency.networkTicker != tx.sourceAccount.currency.networkTicker
                            }
                        }.collectLatest { dexAccounts ->
                            updateState {
                                copy(
                                    accounts = dexAccounts
                                )
                            }
                        }
                }
            }

            is DestinationAccountIntent.OnAccountSelected -> {
                transactionProcessor.updateDestinationAccount(intent.account)
                dexService.updatePersistedDestinationAccount(intent.account)
            }

            is DestinationAccountIntent.Search -> {
                updateState {
                    copy(
                        searchFilter = intent.query
                    )
                }
            }
        }
    }
}

sealed class DestinationAccountIntent : Intent<DestinationAccountModelState> {
    object LoadAccounts : DestinationAccountIntent()
    class OnAccountSelected(val account: DexAccount) : DestinationAccountIntent()
    class Search(val query: String) : DestinationAccountIntent()
}

data class DestinationAccountSelectionViewState(
    val accounts: List<DexAccount> = emptyList()
) : ViewState

data class DestinationAccountModelState(
    val accounts: List<DexAccount> = emptyList(),
    val searchFilter: String = ""
) : ModelState
