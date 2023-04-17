package com.blockchain.transactions.swap.selectsource

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.transactions.common.AccountUiElement
import com.blockchain.transactions.swap.CryptoAccountWithBalance
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class SelectSourceViewModel(
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<SelectSourceIntent, SelectSourceViewState, SelectSourceModelState, EmptyNavEvent, ModelConfigArgs.NoArgs>(
    SelectSourceModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectSourceModelState): SelectSourceViewState {
        return with(state) {
            SelectSourceViewState(
                accountList = accountListData.mapList {
                    it.reduceCryptoAccountWithBalance()
                }
            )
        }
    }

    override suspend fun handleIntent(modelState: SelectSourceModelState, intent: SelectSourceIntent) {
        when (intent) {
            SelectSourceIntent.LoadData -> {
                swapService.custodialSourceAccountsWithBalances().collectLatest { accountListData ->
                    updateState { it.copy(accountListData = accountListData) }
                }
            }
        }
    }

    /*private fun reduceCryptoAccountList(accountList: List<CryptoAccountWithBalance>): List<AccountUiElement> {
        return accountList.map { account ->
            AccountUiElement(
                title = account.cryptoBalance.currency.name,
                subtitle = "", // do we need this?
                valueCrypto = account.cryptoBalance.toStringWithSymbol(),
                valueFiat = account.fiatBalance.toStringWithSymbol(),
                icon = when(account.account) {
                    is NonCustodialAccount -> listOfNotNull(
                        account.cryptoBalance.currency.logo,
                        (account.cryptoBalance.currency as? AssetInfo)?.takeIf { it.isLayer2Token }?.coinNetwork?.nativeAssetTicker
                            ?.let {
                                assetCatalogue.fromNetworkTicker(it)?.logo
                            }
                    )
                    else -> listOf(account.cryptoBalance.currency.logo)
                }
            )
        }*/
    private fun CryptoAccountWithBalance.reduceCryptoAccountWithBalance() = run {
        AccountUiElement(
            title = balanceCrypto.currency.name,
            subtitle = "", // do we need this?
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

