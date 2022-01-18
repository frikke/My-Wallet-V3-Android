package com.blockchain.walletconnect.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget

interface WalletConnectServiceAPI {
    fun attemptToConnect(url: String)
    fun connectToApprovedSessions()
    fun acceptConnection(session: WalletConnectSession)
    fun denyConnection(session: WalletConnectSession)
}

sealed class WalletConnectUserEvent {
    class SingMessage(source: SingleAccount, target: TransactionTarget)
    class SingTransaction(source: SingleAccount, target: TransactionTarget)
    class SendTransaction(source: SingleAccount, target: TransactionTarget)
}

sealed class WalletConnectResponseEvent {
    object Invalid : WalletConnectResponseEvent()
    class Signature(val signature: String) : WalletConnectResponseEvent()
    class TransactionHash(val hash: String) : WalletConnectResponseEvent()
}

sealed class WalletConnectSessionEvent {
    class FailToConnect(val session: WalletConnectSession) : WalletConnectSessionEvent()
    class DidConnect(val session: WalletConnectSession) : WalletConnectSessionEvent()
}
