package com.blockchain.home.presentation.fiat.fundsdetail

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.ActionState
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.StateAwareAction
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.presentation.dashboard.HomeNavEvent
import info.blockchain.balance.FiatCurrency
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await

class FiatFundsDetailViewModel(
    private val currency: FiatCurrency,
    private val coincore: Coincore
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
            FiatFundsDetailIntent.LoadData -> loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val fiatAccount = coincore[currency].defaultAccount().await() as FiatAccount

            updateState {
                it.copy(account = DataResource.Data(fiatAccount))
            }

            combine(
                fiatAccount.balance.map { it.total },
                flowOf(fiatAccount.stateAwareActions.await())
            ) { balance, actions ->
                updateState {
                    it.copy(
                        data = DataResource.Data(
                            FiatFundsDetailData(
                                balance = balance,
                                depositEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw),
                                withdrawEnabled = actions.hasAvailableAction(AssetAction.FiatWithdraw)
                            )
                        )
                    )
                }
            }.catch { error ->
                updateState {
                    it.copy(
                        data = DataResource.Error(Exception(error))
                    )
                }
            }.collect()
        }
    }

    private fun Set<StateAwareAction>.hasAvailableAction(action: AssetAction): Boolean =
        firstOrNull { it.action == action && it.state == ActionState.Available } != null
}
