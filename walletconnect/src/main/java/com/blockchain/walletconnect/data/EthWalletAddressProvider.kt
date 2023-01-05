package com.blockchain.walletconnect.data

import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single

class EthWalletAddressProvider(private val coincore: Coincore) :
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
}
