package com.blockchain.walletconnect.domain

import com.blockchain.coincore.TxResult
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface EthRequestSign {
    fun onEthSign(
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable,
    ): Single<WalletConnectUserEvent.SignMessage>
}

interface EthSendTransactionRequest {
    fun onSendTransaction(
        transaction: WCEthereumTransaction,
        session: WalletConnectSession,
        method: EthereumSendTransactionTarget.Method,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable
    ): Single<WalletConnectUserEvent.SendTransaction>
}
