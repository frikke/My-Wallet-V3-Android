package com.dex.domain

import com.blockchain.coincore.SingleAccount
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class DexAccount(
    val account: SingleAccount,
    val currency: AssetInfo,
    val balance: Money,
    val fiatBalance: Money,
)
