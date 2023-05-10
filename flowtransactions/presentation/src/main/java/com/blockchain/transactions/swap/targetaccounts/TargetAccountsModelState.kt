package com.blockchain.transactions.swap.targetaccounts

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.WithId
import com.blockchain.transactions.swap.CryptoAccountWithBalance

data class TargetAccountsModelState(
    val accountListData: DataResource<List<WithId<CryptoAccountWithBalance>>> = DataResource.Loading
) : ModelState
