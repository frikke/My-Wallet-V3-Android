package com.blockchain.home.presentation.fiat.fundsdetail

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.updateDataWith
import com.blockchain.fiatActions.fiatactions.FiatActions
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.presentation.R
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class FiatFundsDetailViewModel(
    private val fiatTicker: String,
    private val homeAccountsService: HomeAccountsService,
    private val fiatActions: FiatActions
) : MviViewModel<
    FiatFundsDetailIntent,
    FiatFundsDetailViewState,
    FiatFundsDetailModelState,
    FiatFundsDetailNavEvent,
    ModelConfigArgs.NoArgs>(
    FiatFundsDetailModelState()
) {
    companion object {
        const val SNACKBAR_MESSAGE_DURATION: Long = 3000L
    }

    private var snackbarMessageJob: Job? = null
    private var loadDataJob: Job? = null
    private var actionsJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: FiatFundsDetailModelState): FiatFundsDetailViewState = state.run {
        FiatFundsDetailViewState(
            detail = account.map {
                FiatFundsDetail(
                    account = it,
                    name = it.currency.name,
                    logo = it.currency.logo
                )
            },
            data = data,
            showWithdrawChecksLoading = withdrawChecksLoading,
            actionError = when (actionError) {
                FiatActionError.None -> null
                FiatActionError.WithdrawalInProgress -> R.string.fiat_funds_detail_pending_withdrawal
                FiatActionError.Unknown -> R.string.common_error
            }?.let {
                FiatActionErrorState(message = it)
            }
        )
    }

    override suspend fun handleIntent(modelState: FiatFundsDetailModelState, intent: FiatFundsDetailIntent) {
        when (intent) {
            FiatFundsDetailIntent.LoadData -> {
                loadData()
            }

            is FiatFundsDetailIntent.Deposit -> {
                fiatActions.deposit(
                    account = intent.account,
                    action = intent.action,
                    shouldLaunchBankLinkTransfer = intent.shouldLaunchBankLinkTransfer,
                    shouldSkipQuestionnaire = intent.shouldSkipQuestionnaire
                )
            }

            is FiatFundsDetailIntent.Withdraw -> {
                handleWithdraw(intent)
            }
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            homeAccountsService.accounts(WalletMode.CUSTODIAL_ONLY)
                .filterIsInstance<DataResource<List<FiatAccount>>>()
                .mapData { it.first { it.currency.networkTicker == fiatTicker } }
                .onEach { dataResource ->
                    updateState {
                        it.copy(account = it.account.updateDataWith(dataResource))
                    }
                }
                .flatMapData { fiatAccount ->
                    combine(
                        fiatAccount.balance.map { it.total },
                        flowOf(fiatAccount.stateAwareActions.await())
                    ) { balance, actions ->
                        DataResource.Data(
                            FiatFundsDetailData(
                                balance = balance,
                                depositEnabled = actions.hasAvailableAction(AssetAction.FiatDeposit),
                                withdrawEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw)
                            )
                        )
                    }
                }
                .catch {
                    emit(DataResource.Error(Exception(it)))
                }
                .onEach { dataResource ->
                    updateState {
                        it.copy(data = it.data.updateDataWith(dataResource))
                    }
                }
                .collect()
        }

        actionsJob?.cancel()
        actionsJob = viewModelScope.launch {
            fiatActions.result.collectLatest {
                navigate(FiatFundsDetailNavEvent.Dismss)
            }
        }
    }

    private fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
        firstOrNull { it.action == action && it.state == ActionState.Available } != null

    private fun handleWithdraw(intent: FiatFundsDetailIntent.Withdraw) {
        viewModelScope.launch {
            intent.account.canWithdrawFunds()
                .collectLatest { dataResource ->
                    when (dataResource) {
                        DataResource.Loading -> {
                            updateState {
                                it.copy(
                                    withdrawChecksLoading = true,
                                    actionError = FiatActionError.None
                                )
                            }
                        }
                        is DataResource.Data -> {
                            updateState { it.copy(withdrawChecksLoading = false) }

                            dataResource.data.let { canWithdrawFunds ->
                                if (canWithdrawFunds) {
                                    fiatActions.withdraw(
                                        account = intent.account,
                                        action = intent.action,
                                        shouldLaunchBankLinkTransfer = intent.shouldLaunchBankLinkTransfer,
                                        shouldSkipQuestionnaire = intent.shouldSkipQuestionnaire
                                    )
                                } else {
                                    updateState { it.copy(actionError = FiatActionError.WithdrawalInProgress) }
                                    startDismissErrorTimeout()
                                }
                            }
                        }
                        is DataResource.Error -> {
                            updateState {
                                it.copy(
                                    withdrawChecksLoading = false,
                                    actionError = FiatActionError.Unknown
                                )
                            }
                            startDismissErrorTimeout()
                        }
                    }
                }
        }
    }

    private fun startDismissErrorTimeout() {
        snackbarMessageJob?.cancel()
        snackbarMessageJob = viewModelScope.launch {
            delay(SNACKBAR_MESSAGE_DURATION)

            updateState {
                it.copy(actionError = FiatActionError.None)
            }
        }
    }
}
