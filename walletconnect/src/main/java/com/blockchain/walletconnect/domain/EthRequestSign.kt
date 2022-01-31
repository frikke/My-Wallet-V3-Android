package com.blockchain.walletconnect.domain

import com.blockchain.coincore.TxResult
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface EthRequestSign {
    fun onEthSign(
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        onTxCompleted: (TxResult) -> Completable
    ): Single<WalletConnectUserEvent.SignMessage>
}
