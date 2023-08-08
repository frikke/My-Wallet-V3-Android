package com.blockchain.unifiedcryptowallet.domain.balances

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface UnifiedBalancesService {
    fun failedBalancesNetworks(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<CoinNetwork>>>

    fun balances(
        wallet: NetworkWallet? = null,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<NetworkBalance>>>

    fun balanceForWallet(
        wallet: NetworkWallet,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<NetworkBalance>>
}

interface NetworkAccountsService {
    suspend fun allNetworkWallets(): List<NetworkWallet>
    suspend fun activelySupportedNetworks(): List<CoinNetwork>
}

interface CoinNetworksService {
    fun allCoinNetworks(): Flow<DataResource<List<CoinNetwork>>>
}

data class NetworkBalance(
    val currency: Currency,
    val balance: Money,
    val unconfirmedBalance: Money,
    val exchangeRate: ExchangeRate?
) {
    val totalFiat: Money? by lazy {
        exchangeRate?.convert(balance)
    }
}

class UnifiedBalanceNotFoundException(currency: String, index: Int, name: String) :
    IllegalArgumentException("No balance found for $currency for account $name at index $index")
