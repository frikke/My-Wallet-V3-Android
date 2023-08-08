package com.dex.presentation.network

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import info.blockchain.balance.CoinNetwork

data class SelectNetworkModelState(
    val networks: DataResource<List<CoinNetwork>> = DataResource.Loading,
    val selectedChainId: Int
) : ModelState
