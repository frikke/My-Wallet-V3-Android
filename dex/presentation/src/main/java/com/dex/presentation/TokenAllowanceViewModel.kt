package com.dex.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo

class TokenAllowanceViewModel(
    private val assetCatalogue: AssetCatalogue,
) : MviViewModel<
    AllowanceIntent,
    AllowanceViewState,
    AllowanceModelState,
    NavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = AllowanceModelState(
        networkFeeString = null,
        nativeAsset = null,
        assetInfo = null
    )
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {
    }

    override fun reduce(state: AllowanceModelState): AllowanceViewState {
        return with(state) {
            AllowanceViewState(
                networkFee = networkFeeString,
                nativeAsset = nativeAsset,
                assetInfo = assetInfo
            )
        }
    }

    override suspend fun handleIntent(modelState: AllowanceModelState, intent: AllowanceIntent) {
        when (intent) {
            is AllowanceIntent.FetchAllowanceTxDetails -> updateState {
                it.copy(
                    networkFeeString = intent.data.fiatFees,
                    nativeAsset = assetCatalogue.assetInfoFromNetworkTicker(intent.data.networkNativeAssetTicker),
                    assetInfo = assetCatalogue.assetInfoFromNetworkTicker(intent.data.currencyTicker)
                )
            }
        }
    }
}

data class AllowanceModelState(
    val networkFeeString: String?,
    val nativeAsset: AssetInfo?,
    val assetInfo: AssetInfo?,
) : ModelState

data class AllowanceViewState(
    val networkFee: String?,
    val nativeAsset: AssetInfo?,
    val assetInfo: AssetInfo?,
) : ViewState

sealed class AllowanceIntent : Intent<AllowanceModelState> {
    data class FetchAllowanceTxDetails(val data: AllowanceTxUiData) : AllowanceIntent()
}
