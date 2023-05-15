package com.blockchain.walletconnect.domain

import com.blockchain.coincore.SingleAccount
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

interface WalletConnectAddressProvider {
    fun address(): Single<String>
}

interface WalletConnectEthAccountProvider {
    fun account(): Single<SingleAccount>
    fun account(chainId: String): Flow<SingleAccount>
    fun ethAccountFlow(): Flow<SingleAccount>
}
