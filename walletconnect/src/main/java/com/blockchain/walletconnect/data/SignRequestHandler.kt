package com.blockchain.walletconnect.data

import com.blockchain.coincore.TxResult
import com.blockchain.coincore.eth.EthSignMessage
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class SignRequestHandler(
    private val accountProvider: WalletConnectEthAccountProvider
) : EthRequestSign {

    override fun onEthSign(
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        onTxCompleted: (TxResult) -> Completable
    ): Single<WalletConnectUserEvent.SignMessage> {
        return accountProvider.account().map { account ->
            val target = EthereumSignMessageTarget(
                dAppAddress = session.dAppInfo.peerMeta.url,
                dAppName = session.dAppInfo.peerMeta.name,
                dAppLogoUrl = session.dAppInfo.peerMeta.uiIcon(),
                message = message.toEthSignedMessage(),
                onTxCompleted = onTxCompleted,
            )
            WalletConnectUserEvent.SignMessage(
                source = account,
                target = target
            )
        }
    }
}

private fun WCEthereumSignMessage.toEthSignedMessage(): EthSignMessage =
    EthSignMessage(
        raw = this.raw,
        type = when (type) {
            WCEthereumSignMessage.WCSignType.MESSAGE -> EthSignMessage.SignType.MESSAGE
            WCEthereumSignMessage.WCSignType.TYPED_MESSAGE -> EthSignMessage.SignType.TYPED_MESSAGE
            WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE -> EthSignMessage.SignType.PERSONAL_MESSAGE
        }
    )
