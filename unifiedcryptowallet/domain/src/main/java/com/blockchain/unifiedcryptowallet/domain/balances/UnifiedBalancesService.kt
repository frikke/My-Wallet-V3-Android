package com.blockchain.unifiedcryptowallet.domain.balances

import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.lang.IllegalArgumentException

interface UnifiedBalancesService {
    suspend fun balances(wallet: NetworkWallet? = null): List<NetworkBalance>

    suspend fun balanceForWallet(
        wallet: NetworkWallet
    ): NetworkBalance
}

interface NetworkAccountsService {
    suspend fun allNetworks(): List<NetworkWallet>
}

data class NetworkBalance(
    val currency: Currency,
    val balance: Money,
    val unconfirmedBalance: Money,
    val exchangeRate: ExchangeRate
) {
    val totalFiat: Money by lazy {
        exchangeRate.convert(balance)
    }
}

class UnifiedBalanceNotFoundException(currency: String, index: Int, name: String) :
    IllegalArgumentException("No balance found for $currency for account $name at index $index")
