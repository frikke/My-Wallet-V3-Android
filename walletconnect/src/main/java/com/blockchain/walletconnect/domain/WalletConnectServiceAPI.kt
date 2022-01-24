package com.blockchain.walletconnect.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TransactionTarget
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface WalletConnectServiceAPI {
    fun attemptToConnect(url: String): Completable
    fun connectToApprovedSessions()
    fun acceptConnection(session: WalletConnectSession): Completable
    fun denyConnection(session: WalletConnectSession): Completable
    fun disconnect(session: WalletConnectSession): Completable

    val sessionEvents: Observable<WalletConnectSessionEvent>
}

interface WalletConnectUrlValidator {
    fun isUrlValid(url: String): Boolean
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

sealed class WalletConnectSessionEvent(val session: WalletConnectSession) {
    class FailToConnect(session: WalletConnectSession) : WalletConnectSessionEvent(session)
    class DidReject(session: WalletConnectSession) : WalletConnectSessionEvent(session)
    class DidConnect(session: WalletConnectSession) : WalletConnectSessionEvent(session)
    class DidDisconnect(session: WalletConnectSession) : WalletConnectSessionEvent(session)
    class ReadyForApproval(session: WalletConnectSession) : WalletConnectSessionEvent(session)
}
