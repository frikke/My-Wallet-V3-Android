package com.blockchain.transactions.common.sourceaccounts

import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.dataOrElse
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.mapListData
import com.blockchain.data.updateDataWith
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.WithId
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.transactions.common.withId
import com.blockchain.transactions.sell.SellService
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest

class SourceAccountsViewModel(
    private val isSwap: Boolean,
    private val sellService: SellService,
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<
    SourceAccountsIntent,
    SourceAccountsViewState,
    SourceAccountsModelState,
    SourceAccountsNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    SourceAccountsModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun SourceAccountsModelState.reduce() = SourceAccountsViewState(
        accountList = accountListData
            .map { it.sortedByDescending { it.data.balanceFiat } }
            .mapList {
                it.reduceAccounts(
                    includeLabel = accountListData.containsMultipleAccountsOf(
                        it.data.account.currency.networkTicker
                    )
                )
            }
    )

    override suspend fun handleIntent(modelState: SourceAccountsModelState, intent: SourceAccountsIntent) {
        when (intent) {
            is SourceAccountsIntent.LoadData -> {
                val accounts = if (isSwap) {
                    swapService.sourceAccountsWithBalances()
                } else {
                    sellService.sourceAccountsWithBalances()
                }
                accounts
                    .mapListData { it.withId() }
                    .collectLatest { accountListData ->
                        updateState {
                            copy(
                                accountListData = accountListData.updateDataWith(accountListData)
                            )
                        }
                    }
            }

            is SourceAccountsIntent.AccountSelected -> {
                check(modelState.accountListData is DataResource.Data)
                modelState.accountListData.data.run {
                    check(any { it.id == intent.id })
                    val account = first { it.id == intent.id }.data
                    navigate(
                        SourceAccountsNavigationEvent.ConfirmSelection(
                            account = account,
                            requiresSecondPassword = account.account.requireSecondPassword(),
                        )
                    )
                }
            }
        }
    }

    private fun WithId<CryptoAccountWithBalance>.reduceAccounts(includeLabel: Boolean) = run {
        with(data) {
            AccountUiElement(
                id = id,
                title = account.currency.name,
                subtitle = account.label.takeIf { includeLabel },
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

private fun DataResource<List<WithId<CryptoAccountWithBalance>>>.containsMultipleAccountsOf(
    ticker: String
): Boolean {
    return map {
        it.count { it.data.account.currency.networkTicker == ticker }
    }.dataOrElse(0) > 1
}
