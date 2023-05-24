package com.blockchain.transactions.common.entersecondpassword

import com.blockchain.transactions.swap.CryptoAccountWithBalance

class EnterSecondPasswordArgs(
    private var _sourceAccount: CryptoAccountWithBalance? = null,
) {
    val sourceAccount: CryptoAccountWithBalance
        get() = _sourceAccount.let {
            check(it != null)
            it
        }

    fun update(
        sourceAccount: CryptoAccountWithBalance,
    ) {
        this._sourceAccount = sourceAccount
    }

    fun reset() {
        _sourceAccount = null
    }
}
