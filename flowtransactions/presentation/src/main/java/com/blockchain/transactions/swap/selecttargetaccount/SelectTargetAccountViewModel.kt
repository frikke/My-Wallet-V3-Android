package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
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
import com.blockchain.transactions.swap.selectsource.SelectSourceNavigationEvent
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isLayer2Token
import kotlinx.coroutines.flow.collectLatest

class SelectTargetAccountViewModel(
    private val sourceTicker: String,
    private val targetTicker: String,
    private val mode: WalletMode,
    private val swapService: SwapService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<SelectTargetAccountIntent,
    SelectTargetAccountViewState,
    SelectTargetAccountModelState,
    TargetAccountNavigationEvent,
    ModelConfigArgs.NoArgs>(
    SelectTargetAccountModelState()
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun reduce(state: SelectTargetAccountModelState) = state.run {
        SelectTargetAccountViewState(
            accountList = accountListData
                .map { it.sortedByDescending { it.data.balanceFiat } }
                .mapList { it.reduceAccounts() }
        )
    }

    override suspend fun handleIntent(
        modelState: SelectTargetAccountModelState,
        intent: SelectTargetAccountIntent
    ) {
        when (intent) {
            is SelectTargetAccountIntent.LoadData -> {
                swapService
                    .accountsWithBalanceOfMode(
                        sourceTicker = sourceTicker,
                        selectedAssetTicker = targetTicker,
                        mode = mode
                    )
                    .mapListData { it.withId() }
                    .collectLatest { accountsData ->
                        updateState {
                            it.copy(
                                accountListData = it.accountListData.updateDataWith(accountsData)
                            )
                        }
                    }
            }

            is SelectTargetAccountIntent.AccountSelected -> {
                check(modelState.accountListData is DataResource.Data)
                modelState.accountListData.data.run {
                    check(any { it.id == intent.id })
                    navigate(
                        TargetAccountNavigationEvent.ConfirmSelection(
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