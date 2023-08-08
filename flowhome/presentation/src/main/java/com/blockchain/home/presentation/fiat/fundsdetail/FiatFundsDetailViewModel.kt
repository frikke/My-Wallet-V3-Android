package com.blockchain.home.presentation.fiat.fundsdetail

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FiatAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.filterDataIsInstance
import com.blockchain.data.flatMapData
import com.blockchain.data.map
import com.blockchain.data.mapData
import com.blockchain.data.updateDataWith
import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.fiat.actions.hasAvailableAction
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class FiatFundsDetailViewModel(
    private val fiatTicker: String,
    private val homeAccountsService: HomeAccountsService,
    private val fiatActions: FiatActionsUseCase
) : MviViewModel<
    FiatFundsDetailIntent,
    FiatFundsDetailViewState,
    FiatFundsDetailModelState,
    FiatFundsDetailNavEvent,
    ModelConfigArgs.NoArgs
    >(
    FiatFundsDetailModelState()
) {
    companion object {
        const val SNACKBAR_MESSAGE_DURATION: Long = 3000L
    }

    private var snackbarMessageJob: Job? = null
    private var loadDataJob: Job? = null
    private var actionsJob: Job? = null

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun FiatFundsDetailModelState.reduce() = FiatFundsDetailViewState(
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
            FiatActionError.WithdrawalInProgress ->
                com.blockchain.stringResources.R.string.fiat_funds_detail_pending_withdrawal

            FiatActionError.Unknown -> com.blockchain.stringResources.R.string.common_error
        }?.let {
            FiatActionErrorState(message = it)
        }
    )

    override suspend fun handleIntent(modelState: FiatFundsDetailModelState, intent: FiatFundsDetailIntent) {
        when (intent) {
            FiatFundsDetailIntent.LoadData -> {
                loadData()
            }

            is FiatFundsDetailIntent.FiatAction -> {
                when (intent.action) {
                    AssetAction.FiatDeposit -> fiatActions.deposit(
                        account = intent.account,
                        action = intent.action,
                        shouldLaunchBankLinkTransfer = false,
                        shouldSkipQuestionnaire = false
                    )

                    AssetAction.FiatWithdraw -> handleWithdraw(intent)
                    else -> error("unsupported")
                }
            }
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            homeAccountsService.accounts(WalletMode.CUSTODIAL)
                .filterDataIsInstance<FiatAccount>()
                .mapData { it.first { it.currency.networkTicker == fiatTicker } }
                .onEach { dataResource ->
                    updateState {
                        copy(account = account.updateDataWith(dataResource))
                    }
                }
                .flatMapData { fiatAccount ->
                    combine(
                        fiatAccount.balance().map { it.total },
                        flow {
                            emit(fiatAccount.stateAwareActions.await())
                        }.catch {
                            emit(emptySet())
                        }
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
                        copy(data = data.updateDataWith(dataResource))
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

    private fun handleWithdraw(intent: FiatFundsDetailIntent.FiatAction) {
        require(intent.action == AssetAction.FiatWithdraw) { "action is not AssetAction.FiatWithdraw" }

        viewModelScope.launch {
            intent.account.canWithdrawFunds()
                .collectLatest { dataResource ->
                    when (dataResource) {
                        DataResource.Loading -> {
                            updateState {
                                copy(
                                    withdrawChecksLoading = true,
                                    actionError = FiatActionError.None
                                )
                            }
                        }

                        is DataResource.Data -> {
                            updateState { copy(withdrawChecksLoading = false) }

                            dataResource.data.let { canWithdrawFunds ->
                                if (canWithdrawFunds) {
                                    fiatActions.withdraw(
                                        account = intent.account,
                                        action = intent.action,
                                        shouldLaunchBankLinkTransfer = false,
                                        shouldSkipQuestionnaire = false
                                    )
                                } else {
                                    updateState { copy(actionError = FiatActionError.WithdrawalInProgress) }
                                    startDismissErrorTimeout()
                                }
                            }
                        }

                        is DataResource.Error -> {
                            updateState {
                                copy(
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
                copy(actionError = FiatActionError.None)
            }
        }
    }
}
