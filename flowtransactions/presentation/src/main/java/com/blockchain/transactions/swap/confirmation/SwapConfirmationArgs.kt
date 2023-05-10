package com.blockchain.transactions.swap.confirmation

import com.blockchain.coincore.CryptoAccount
import info.blockchain.balance.CryptoValue

class SwapConfirmationArgs(
    private var _sourceAccount: CryptoAccount? = null,
    private var _targetAccount: CryptoAccount? = null,
    private var _sourceCryptoAmount: CryptoValue? = null,
    // TODO(aromano): SWAP temp comment, this is only going to be used for NC->* swaps
    private var _secondPassword: String? = null,
) {
    val sourceAccount: CryptoAccount
        get() = _sourceAccount.let {
            check(it != null)
            it
        }

    val targetAccount: CryptoAccount
        get() = _targetAccount.let {
            check(it != null)
            it
        }

    val sourceCryptoAmount: CryptoValue
        get() = _sourceCryptoAmount.let {
            check(it != null)
            it
        }

    val secondPassword: String? get() = _secondPassword

    fun update(
        sourceAccount: CryptoAccount,
        targetAccount: CryptoAccount,
        sourceCryptoAmount: CryptoValue,
        secondPassword: String? = null,
    ) {
        this._sourceAccount = sourceAccount
        this._targetAccount = targetAccount
        this._sourceCryptoAmount = sourceCryptoAmount
        this._secondPassword = secondPassword
    }

    fun reset() {
        _sourceAccount = null
        _targetAccount = null
        _sourceCryptoAmount = null
        _secondPassword = null
    }
}
