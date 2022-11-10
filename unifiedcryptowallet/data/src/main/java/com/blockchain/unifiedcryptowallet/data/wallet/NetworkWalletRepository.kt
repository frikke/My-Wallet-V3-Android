package com.blockchain.unifiedcryptowallet.data.wallet

import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.getOrNull
import com.blockchain.outcome.map
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletGroup
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletService
import info.blockchain.balance.AssetInfo

class NetworkWalletRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val networksService: CoinNetworksService
) : NetworkWalletService {

    override suspend fun networkWalletGroup(currency: String): NetworkWalletGroup? {
        val wallets = networkAccountsService.allNetworkWallets()

        return networksService.allCoinNetworks().map { networks ->
            networks.find { network ->
                network.currency == currency
            }?.let { network ->
                wallets.find { it.currency.networkTicker == currency }?.let {
                    NetworkWalletGroup(
                        parentChainNetwork = it,
                        name = network.name,
                        networkWallets = wallets.networkWalletsForGroup(currency)
                    )
                }
            }
        }.getOrNull()
    }

    override suspend fun networkWalletGroups(): List<NetworkWalletGroup> {

        val wallets = networkAccountsService.allNetworkWallets()

        return networksService.allCoinNetworks().map { networks ->
            networks.mapNotNull { network ->
                wallets.find { it.currency.networkTicker == network.currency }?.let {
                    NetworkWalletGroup(
                        parentChainNetwork = it,
                        name = network.name,
                        networkWallets = wallets.networkWalletsForGroup(network.currency)
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun List<NetworkWallet>.networkWalletsForGroup(network: String): List<NetworkWallet> {
        return filter { wallet ->
            val currency = wallet.currency
            currency is AssetInfo && currency.l1chainTicker == network
        }
    }
}
