package com.blockchain.transactions.receive.detail

import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource

data class ReceiveAccountDetailModelState(
    val account: SingleAccount,
    val receiveAddress: DataResource<CryptoAddress> = DataResource.Loading,
    val isRotatingAddress: Boolean = false
) : ModelState
