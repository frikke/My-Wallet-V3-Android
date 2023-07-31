package com.blockchain.transactions.sell.sourceaccounts

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.CryptoAccountWithBalance
import com.blockchain.transactions.common.WithId

data class SellSourceAccountsModelState(
    val accountListData: DataResource<List<WithId<CryptoAccountWithBalance>>> = DataResource.Loading
) : ModelState
