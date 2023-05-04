package com.blockchain.transactions.swap.targetassets

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.prices.domain.AssetPriceInfo
import com.blockchain.walletmode.WalletMode

data class SelectTargetModelState(
    val walletMode: WalletMode? = null,
    val selectedAssetsModeFilter: WalletMode? = null,
    val prices: DataResource<List<AssetPriceInfo>> = DataResource.Loading,
    val filterTerm: String = "",
) : ModelState
