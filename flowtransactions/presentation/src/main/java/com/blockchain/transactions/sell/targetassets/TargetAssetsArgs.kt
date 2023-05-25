package com.blockchain.transactions.sell.targetassets

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.transactions.common.CryptoAccountWithBalance
import java.io.Serializable

data class TargetAssetsArgs(
    val sourceAccount: Bindable<CryptoAccountWithBalance>,
    val secondPassword: String?,
) : Serializable
