package com.blockchain.walletconnect.data

import com.blockchain.coincore.Coincore
import com.blockchain.walletconnect.domain.WalletConnectAddressProvider
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single

class EthWalletAddressProvider(private val coincore: Coincore) : WalletConnectAddressProvider {
    override fun address(): Single<String> = coincore[CryptoCurrency.ETHER].defaultAccount().flatMap {
        it.receiveAddress.map { receiveAddress ->
            receiveAddress.address
        }
    }
}
