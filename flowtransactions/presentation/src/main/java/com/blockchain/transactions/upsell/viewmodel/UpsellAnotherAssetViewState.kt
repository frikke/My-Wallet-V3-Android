package com.blockchain.transactions.upsell.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource
import com.blockchain.prices.prices.PriceItemViewState
import kotlinx.collections.immutable.ImmutableList

data class UpsellAnotherAssetViewState(
    val assetsToUpSell: DataResource<ImmutableList<PriceItemViewState>>,
    val isLoading: Boolean = true
) : ViewState
