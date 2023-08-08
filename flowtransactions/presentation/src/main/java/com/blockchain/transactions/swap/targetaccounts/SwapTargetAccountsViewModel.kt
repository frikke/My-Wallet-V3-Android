package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.map
import com.blockchain.data.mapList
import com.blockchain.data.mapListData
import com.blockchain.data.updateDataWith
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.WithId
import com.blockchain.transactions.common.accounts.AccountUiElement
import com.blockchain.transactions.common.withId
import com.blockchain.transactions.swap.SwapService
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest

class SwapTargetAccountsViewModel(
    private val sourceTicker: String,
    private val targetTicker: String,
    private val mode: WalletMode,
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<
    SwapTargetAccountsIntent,
    SwapTargetAccountsViewState,
    SwapTargetAccountsModelState,
    SwapTargetAccountsNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    SwapTargetAccountsModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun SwapTargetAccountsModelState.reduce() = SwapTargetAccountsViewState(
        accountList = accountListData
            .map { it.sortedByDescending { it.data.balanceFiat } }
            .mapList { it.reduceAccounts() }
    )

    override suspend fun handleIntent(
        modelState: SwapTargetAccountsModelState,
        intent: SwapTargetAccountsIntent
    ) {
        when (intent) {
            is SwapTargetAccountsIntent.LoadData -> {
                swapService
                    .targetAccountsWithBalanceOfMode(
                        sourceTicker = sourceTicker,
                        selectedAssetTicker = targetTicker,
                        mode = mode
                    )
                    .mapListData { it.withId() }
                    .collectLatest { accountsData ->
                        updateState {
                            copy(
                                accountListData = accountListData.updateDataWith(accountsData)
                            )
                        }
                    }
            }

            is SwapTargetAccountsIntent.AccountSelected -> {
                check(modelState.accountListData is DataResource.Data)
                modelState.accountListData.data.run {
                    check(any { it.id == intent.id })
                    navigate(
                        SwapTargetAccountsNavigationEvent.ConfirmSelection(
                            account = first { it.id == intent.id }.data.account
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
                title = account.label,
                subtitle = account.currency.name,
                l2Network = (account as? CryptoNonCustodialAccount)?.currency
                    ?.takeIf { it.isLayer2Token }
                    ?.coinNetwork?.shortName,
                valueCrypto = balanceCrypto.toStringWithSymbol(),
                valueFiat = balanceFiat.toStringWithSymbol(),
                icon = balanceCrypto.currency.logo,
                nativeAssetIcon = (account as? CryptoNonCustodialAccount)?.currency
                    ?.takeIf { it.isLayer2Token }
                    ?.coinNetwork?.nativeAssetTicker
                    ?.let { assetCatalogue.fromNetworkTicker(it)?.logo },
            )
        }
    }
}
