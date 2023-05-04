package com.blockchain.transactions.swap.selecttargetaccount

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.WithId
import com.blockchain.transactions.swap.CryptoAccountWithBalance

data class SelectTargetAccountModelState(
    val accountListData: DataResource<List<WithId<CryptoAccountWithBalance>>> = DataResource.Loading
) : ModelState
