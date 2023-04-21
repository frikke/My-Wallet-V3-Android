package com.blockchain.transactions.swap.selectsource

import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.transactions.common.AccountUiElement
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
    EmptyNavEvent,
    ModelConfigArgs.NoArgs>(
    SelectSourceModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectSourceModelState): SelectSourceViewState {
        return with(state) {
            SelectSourceViewState(
                accountList = accountListData
                    .map { it.sortedByDescending { it.balanceFiat } }
                    .mapList { it.reduceAccounts() }
            )
        }
    }

    override suspend fun handleIntent(modelState: SelectSourceModelState, intent: SelectSourceIntent) {
        when (intent) {
            is SelectSourceIntent.LoadData -> {
                swapService.custodialSourceAccountsWithBalances().collectLatest { accountListData ->
                    updateState {
                        it.copy(
                            accountListData = it.accountListData.updateDataWith(accountListData)
                        )
                    }
                }
            }
        }
    }

    private fun CryptoAccountWithBalance.reduceAccounts() = run {
        AccountUiElement(
            ticker = account.currency.networkTicker,
            assetName = account.currency.name,
            l2Network = (account as? CryptoNonCustodialAccount)?.currency
                ?.takeIf { it.isLayer2Token }
                ?.coinNetwork?.shortName,
            valueCrypto = balanceCrypto.toStringWithSymbol(),
            valueFiat = balanceFiat.toStringWithSymbol(),
            icon = when (account) {
                is NonCustodialAccount -> listOfNotNull(
                    balanceCrypto.currency.logo,
                    (balanceCrypto.currency as? AssetInfo)?.takeIf { it.isLayer2Token }?.coinNetwork?.nativeAssetTicker
                        ?.let {
                            assetCatalogue.fromNetworkTicker(it)?.logo
                        }
                )
                else -> listOf(balanceCrypto.currency.logo)
            }
        )
    }
}
