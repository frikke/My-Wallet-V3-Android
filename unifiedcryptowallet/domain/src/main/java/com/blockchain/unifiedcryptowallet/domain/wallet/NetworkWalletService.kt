package com.blockchain.unifiedcryptowallet.domain.wallet

/**
 * Todo on a next PR the implementation
 */
interface NetworkWalletService {
    suspend fun networkWalletGroup(currency: String): NetworkWalletGroup?
    suspend fun networkWalletGroups(): List<NetworkWalletGroup>
}
