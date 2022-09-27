package com.blockchain.home.presentation.ui

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.home.domain.HomeAccountsService
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class HomeViewModel(
    private val homeAccountsService: HomeAccountsService
) :
    MviViewModel<HomeIntent, HomeViewState, HomeModelState, HomeNavEvent, ModelConfigArgs.NoArgs>(
        HomeModelState(accounts = DataResource.Data(emptyList()))
    ) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
        updateState { state ->
            state.copy(accounts = DataResource.Data(emptyList()))
        }
    }

    override fun reduce(state: HomeModelState): HomeViewState {
        return with(state) {
            HomeViewState(
                balance = accounts.totalBalance(),
                homeAssets = DataResource.Data(emptyList()),
                activity = DataResource.Data(emptyList())
            )
        }
    }

    override suspend fun handleIntent(modelState: HomeModelState, intent: HomeIntent) {
        when (intent) {
            HomeIntent.LoadHomeAccounts -> loadAccounts()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadAccounts() {
        homeAccountsService.accounts().onEach {
            updateState { state ->
                state.copy(
                    accounts = it.map { accounts ->
                        accounts.map {
                            ModelAccount(
                                blockchainAccount = it,
                                balance = DataResource.Loading,
                                fiatBalance = DataResource.Loading
                            )
                        }
                    }
                )
            }
        }.filterIsInstance<DataResource.Data<List<BlockchainAccount>>>()
            .flatMapLatest { accounts ->
                val balances = accounts.data.map { account ->
                    account.balance.map { balance ->
                        account to balance
                    }
                }
                balances.merge().onEach { (account, balance) ->
                    updateState { state ->
                        state.copy(
                            accounts =
                            state.accounts.withBalancedAccount(account, balance)
                        )
                    }
                }
            }.collect()
    }
}

private fun DataResource<List<ModelAccount>>.totalBalance(): DataResource<Money> {
    return this.map {
        it.totalAccounts()
    }
}

private fun List<ModelAccount>.totalAccounts(): Money {
    return map { it.fiatBalance }.filterIsInstance<DataResource.Data<Money>>()
        .map { it.data }
        .fold(Money.zero(Dollars)) { acc, t ->
            acc.plus(t)
        }
}

private fun DataResource<List<ModelAccount>>.withBalancedAccount(
    account: BlockchainAccount,
    balance: AccountBalance
): DataResource<List<ModelAccount>> {
    return this.map { modelAccounts ->
        modelAccounts.filterNot {
            it.blockchainAccount == account
        }.plus(
            ModelAccount(
                blockchainAccount = account,
                balance = DataResource.Data(balance.total),
                fiatBalance = DataResource.Data(balance.totalFiat),
            )
        )
    }
}

sealed class HomeIntent : Intent<HomeModelState> {
    object LoadHomeAccounts : HomeIntent()
}

data class HomeModelState(
    val accounts: DataResource<List<ModelAccount>>
) : ModelState

data class ModelAccount(
    val blockchainAccount: BlockchainAccount,
    val balance: DataResource<Money>,
    val fiatBalance: DataResource<Money>,
)

data class HomeViewState(
    val balance: DataResource<Money>,
    val homeAssets: DataResource<List<HomeAsset>>,
    val activity: DataResource<List<HomeActivity>>
) : ViewState

sealed class HomeNavEvent : NavigationEvent

sealed interface HomeAsset {
    val currency: Currency
    val balance: DataResource<Money>
    val userFiatBalance: DataResource<Money>
}

data class HomeCryptoAsset(
    override val currency: Currency,
    override val balance: DataResource<Money>,
    override val userFiatBalance: DataResource<Money>
) : HomeAsset

data class HomeFiatAsset(
    override val currency: Currency,
    override val balance: DataResource<Money>,
    override val userFiatBalance: DataResource<Money>
) : HomeAsset

data class HomeActivity(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val amount: Money,
    val userFiatAmount: Money
)
