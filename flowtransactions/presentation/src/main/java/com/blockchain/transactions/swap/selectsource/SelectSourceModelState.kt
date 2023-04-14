package com.blockchain.transactions.swap.selectsource

import com.blockchain.coincore.CryptoAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.transactions.swap.SwapService.CryptoAccountWithBalance
import info.blockchain.balance.Money

data class SelectSourceModelState(
    val accountListData: List<DataResource<CryptoAccountWithBalance>> = listOf(DataResource.Loading)
) : ModelState