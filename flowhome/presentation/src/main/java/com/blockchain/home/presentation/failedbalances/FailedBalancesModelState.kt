package com.blockchain.home.presentation.failedbalances

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import info.blockchain.balance.CoinNetwork

data class FailedBalancesModelState(
    val failedBalancesNetworks: DataResource<List<CoinNetwork>> = DataResource.Loading,
    val dismissFailedNetworksWarning: Boolean = false,
    val lastFreshDataTime: Long = 0
) : ModelState
