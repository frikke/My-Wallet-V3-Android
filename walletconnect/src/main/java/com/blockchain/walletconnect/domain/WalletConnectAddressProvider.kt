package com.blockchain.walletconnect.domain

import io.reactivex.rxjava3.core.Single

interface WalletConnectAddressProvider {
    fun address(): Single<String>
}
