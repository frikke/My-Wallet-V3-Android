package com.blockchain.unifiedcryptowallet.data.wallet

import com.blockchain.data.DataResource
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletGroup
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletService
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

class NetworkWalletRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val networksService: CoinNetworksService
) : NetworkWalletService {

    override fun networkWalletGroup(currency: String): Flow<DataResource<NetworkWalletGroup>> {

        TODO()
    }

    override fun networkWalletGroups(): Flow<DataResource<List<NetworkWalletGroup>>> {

        TODO()
    }
}

private fun List<NetworkWallet>.networkWalletsForGroup(network: String): List<NetworkWallet> {
    return filter { wallet ->
        val currency = wallet.currency
        currency is AssetInfo && currency.l1chainTicker == network
    }
}
