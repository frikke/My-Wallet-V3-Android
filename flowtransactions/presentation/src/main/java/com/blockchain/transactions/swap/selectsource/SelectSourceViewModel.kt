package com.blockchain.transactions.swap.selectsource

import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.store.mapListData
import com.blockchain.transactions.common.WithId
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.transactions.common.withId
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest

class SelectSourceViewModel(
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<SelectSourceIntent,
    SelectSourceViewState,
    SelectSourceModelState,
    SelectSourceNavigationEvent,
    ModelConfigArgs.NoArgs>(
    SelectSourceModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectSourceModelState): SelectSourceViewState {
        return with(state) {
            SelectSourceViewState(
                accountList = accountListData
                    .map { it.sortedByDescending { it.data.balanceFiat } }
                    .mapList { it.reduceAccounts() }
            )
        }
    }

    override suspend fun handleIntent(modelState: SelectSourceModelState, intent: SelectSourceIntent) {
        when (intent) {
            is SelectSourceIntent.LoadData -> {
                swapService.sourceAccountsWithBalances()
                    .mapListData { it.withId() }
                    .collectLatest { accountListData ->
                        updateState {
                            it.copy(
                                accountListData = it.accountListData.updateDataWith(accountListData)
                            )
                        }
                    }
            }

            is SelectSourceIntent.AccountSelected -> {
                check(modelState.accountListData is DataResource.Data)
                modelState.accountListData.data.run {
                    check(any { it.id == intent.id })
                    navigate(
                        SelectSourceNavigationEvent.ConfirmSelection(
                            account = first { it.id == intent.id }.data
                        )
                    )
                }
            }
        }
    }

    private fun WithId<CryptoAccountWithBalance>.reduceAccounts() = run {
        with(data) {
            AccountUiElement(
                id = id,
                assetName = account.currency.name,
                l2Network = (account as? CryptoNonCustodialAccount)?.currency
                    ?.takeIf { it.isLayer2Token }
                    ?.coinNetwork?.shortName,
                valueCrypto = balanceCrypto.toStringWithSymbol(),
                valueFiat = balanceFiat.toStringWithSymbol(),
                icon = when (account) {
                    is NonCustodialAccount -> listOfNotNull(
                        balanceCrypto.currency.logo,
                        (balanceCrypto.currency as? AssetInfo)
                            ?.takeIf { it.isLayer2Token }
                            ?.coinNetwork?.nativeAssetTicker
                            ?.let { assetCatalogue.fromNetworkTicker(it)?.logo }
                    )
                    else -> listOf(balanceCrypto.currency.logo)
                }
            )
        }
    }
}
