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
import com.blockchain.home.domain.HomeAccountsService
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.fiatActions.fiatactions.FiatActions
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
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
    FiatFundsDetailIntent, FiatFundsDetailViewState, FiatFundsDetailModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
    FiatFundsDetailModelState()
) {
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
            data = data
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
        }
    }

    private fun loadData() {
        viewModelScope.launch {
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
                                depositEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw),
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

        //        viewModelScope.launch {
        //            val fiatAccount = coincore[currency].defaultAccount().await() as FiatAccount
        //
        //            updateState {
        //                it.copy(account = DataResource.Data(fiatAccount))
        //            }
        //
        //            combine(
        //                fiatAccount.balance.map { it.total },
        //                flowOf(fiatAccount.stateAwareActions.await())
        //            ) { balance, actions ->
        //                updateState {
        //                    it.copy(
        //                        data = DataResource.Data(
        //                            FiatFundsDetailData(
        //                                balance = balance,
        //                                depositEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw),
        //                                withdrawEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw)
        //                            )
        //                        )
        //                    )
        //                }
        //            }.catch { error ->
        //                updateState {
        //                    it.copy(
        //                        data = DataResource.Error(Exception(error))
        //                    )
        //                }
        //            }.collect()
        //        }
    }

    private fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
        firstOrNull { it.action == action && it.state == ActionState.Available } != null
}
