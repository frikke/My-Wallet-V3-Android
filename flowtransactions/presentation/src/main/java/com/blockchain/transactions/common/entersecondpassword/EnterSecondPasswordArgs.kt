package com.blockchain.transactions.common.entersecondpassword

import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.transactions.common.CryptoAccountWithBalance
import java.io.Serializable

data class EnterSecondPasswordArgs(
    val sourceAccount: Bindable<CryptoAccountWithBalance>
) : Serializable
