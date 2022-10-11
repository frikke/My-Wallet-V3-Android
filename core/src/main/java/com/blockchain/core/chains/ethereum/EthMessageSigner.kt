package com.blockchain.core.chains.ethereum

import io.reactivex.rxjava3.core.Single

interface EthMessageSigner {
    fun signEthMessage(message: String, secondPassword: String = ""): Single<ByteArray>
    fun signEthTypedMessage(message: String, secondPassword: String = ""): Single<ByteArray>
}
