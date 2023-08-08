package com.blockchain.coincore

interface OneTimeAccountPersistenceService {
    fun saveAccount(selectedAccount: SingleAccount)

    /**
     * account is destroyed after this call to avoid not setting for future usage
     */
    fun getAccount(): SingleAccount
}

internal object OneTimeAccountPersistenceRepository : OneTimeAccountPersistenceService {
    private var selectedAccount: SingleAccount = NullCryptoAccount()

    override fun saveAccount(selectedAccount: SingleAccount) {
        this.selectedAccount = selectedAccount
    }

    override fun getAccount(): SingleAccount {
        return selectedAccount.also { selectedAccount = NullCryptoAccount() }
    }
}
