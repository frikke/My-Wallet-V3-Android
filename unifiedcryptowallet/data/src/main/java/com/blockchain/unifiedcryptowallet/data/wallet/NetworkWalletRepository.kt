package com.blockchain.unifiedcryptowallet.data.wallet

import com.blockchain.data.DataResource
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletGroup
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWalletService
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest

class NetworkWalletRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val networksService: CoinNetworksService
) : NetworkWalletService {

    override fun networkWalletGroup(currency: String): Flow<DataResource<NetworkWalletGroup>> {

        return networkAccountsService.allNetworkWallets().flatMapLatest { wallets ->
            networksService.allCoinNetworks().mapData { coinNetworks ->
                coinNetworks.find { network ->
                    network.currency == currency
                }?.let { network ->
                    wallets.find { it.currency.networkTicker == network.currency }?.let {
                        NetworkWalletGroup(
                            parentChainNetwork = it,
                            name = network.name,
                            networkWallets = wallets.networkWalletsForGroup(network.currency)
                        )
                    } ?: throw Exception("Parent NetworkWallet not found for $currency")
                } ?: throw Exception("CoinNetwork not found for $currency")
            }
        }
            .catch {
                emit(DataResource.Error(Exception(it)))
            }
    }

    override fun networkWalletGroups(): Flow<DataResource<List<NetworkWalletGroup>>> {

        return networkAccountsService.allNetworkWallets().flatMapLatest { wallets ->
            networksService.allCoinNetworks().mapData {
                it.mapNotNull { network ->
                    wallets.find { wallet ->
                        wallet.currency.networkTicker == network.currency
                    }?.let { networkWallet ->
                        NetworkWalletGroup(
                            parentChainNetwork = networkWallet,
                            name = network.name,
                            networkWallets = wallets.networkWalletsForGroup(network.currency)
                        )
                    }
                }
            }
        }
    }

    private fun List<NetworkWallet>.networkWalletsForGroup(network: String): List<NetworkWallet> {
        return filter { wallet ->
            val currency = wallet.currency
            currency is AssetInfo && currency.l1chainTicker == network
        }
    }
}
