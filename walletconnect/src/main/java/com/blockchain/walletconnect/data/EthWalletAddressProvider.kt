package com.blockchain.walletconnect.data

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.utils.asFlow
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class EthWalletAddressProvider(
    private val coincore: Coincore,
    private val ethDataManager: EthDataManager,
) :
    WalletConnectAddressProvider,
    WalletConnectEthAccountProvider {
    override fun address(): Single<String> =
        coincore[CryptoCurrency.ETHER].defaultAccount(filter = AssetFilter.NonCustodial).flatMap {
            it.receiveAddress.map { receiveAddress ->
                receiveAddress.address
            }
        }

    override fun account(): Single<SingleAccount> =
        coincore[CryptoCurrency.ETHER].defaultAccount(filter = AssetFilter.NonCustodial)

    override fun ethAccountFlow(): Flow<SingleAccount> =
        coincore[CryptoCurrency.ETHER].defaultAccount(filter = AssetFilter.NonCustodial).asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun account(chainId: String): Flow<SingleAccount> {
        val supportedEvmNetworksFlow = getSupportedEvmNetworks().asFlow().map { supportedEvmNetworks ->
            supportedEvmNetworks.first { coinNetwork ->
                coinNetwork.chainId?.let {
                    it == chainId.split(":").last().toInt()
                } ?: false
            }
        }

        return supportedEvmNetworksFlow.flatMapLatest {
            coincore.activeWalletsInMode(
                walletMode = WalletMode.NON_CUSTODIAL,
            ).map {
                it.accounts
            }.map {
                it.filterIsInstance<CryptoAccount>()
            }.map { accounts ->
                accounts.filter { account ->
                    account.currency.coinNetwork == it
                }
            }.distinctUntilChanged().map { accountList ->
                accountList.first { it.isDefault }
            }
        }
    }

    private fun getSupportedEvmNetworks() = ethDataManager.supportedNetworks
}
