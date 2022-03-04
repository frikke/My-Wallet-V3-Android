package com.blockchain.walletconnect.domain

import com.blockchain.coincore.SingleAccount
import io.reactivex.rxjava3.core.Single

interface WalletConnectAddressProvider {
    fun address(): Single<String>
}

interface WalletConnectEthAccountProvider {
    fun account(): Single<SingleAccount>
}
