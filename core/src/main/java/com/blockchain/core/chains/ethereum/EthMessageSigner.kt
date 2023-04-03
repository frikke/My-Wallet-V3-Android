package com.blockchain.core.chains.ethereum

import com.blockchain.core.chains.dynamicselfcustody.domain.model.PreImage
import com.blockchain.core.chains.dynamicselfcustody.domain.model.TransactionSignature
import io.reactivex.rxjava3.core.Single

interface EthMessageSigner {
    fun signEthMessage(message: String, secondPassword: String = ""): Single<ByteArray>
    fun signEthTypedMessage(message: String, secondPassword: String = ""): Single<ByteArray>
}

interface EvmNetworkPreImageSigner {
    fun signPreImage(preImage: PreImage): TransactionSignature
}
