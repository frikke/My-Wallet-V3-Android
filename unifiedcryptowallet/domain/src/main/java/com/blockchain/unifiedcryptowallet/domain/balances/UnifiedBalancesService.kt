package com.blockchain.unifiedcryptowallet.domain.balances

import com.blockchain.data.DataResource
import com.blockchain.domain.wallet.CoinNetwork
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.lang.IllegalArgumentException
import kotlinx.coroutines.flow.Flow

interface UnifiedBalancesService {
    fun balances(wallet: NetworkWallet? = null): Flow<DataResource<List<NetworkBalance>>>

    fun balanceForWallet(
        wallet: NetworkWallet
    ): Flow<DataResource<NetworkBalance>>
}

interface NetworkAccountsService {
    fun allNetworkWallets(): Flow<List<NetworkWallet>>
}

interface CoinNetworksService {
    fun allCoinNetworks(): Flow<DataResource<List<CoinNetwork>>>
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
