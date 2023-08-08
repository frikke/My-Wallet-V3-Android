package com.blockchain.transactions.receive.accounts

import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.transactions.common.WithId

data class ReceiveAccountsModelState(
    val accounts: DataResource<List<WithId<SingleAccount>>> = DataResource.Loading,
    val searchTerm: String = ""
) : ModelState
