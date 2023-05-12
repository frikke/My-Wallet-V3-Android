package com.blockchain.walletconnect.domain

import com.blockchain.coincore.TxResult
import com.walletconnect.web3.wallet.client.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface WalletConnectV2Service {

    val walletEvents: SharedFlow<Wallet.Model>
    val userEvents: SharedFlow<WalletConnectUserEvent>

    fun init()

    suspend fun pair(pairingUrl: String)

    fun buildApprovedSessionNamespaces(
        sessionProposal: Wallet.Model.SessionProposal
    ): Flow<Map<String, Wallet.Model.Namespace.Session>>
    fun approveLastSession()
    suspend fun getSessions(): List<WalletConnectSession>
    fun getSessionsFlow(): Flow<List<WalletConnectSession>>
    suspend fun disconnectSession(sessionTopic: String)
    suspend fun disconnectAllSessions()
    fun clearSessionProposals()
    suspend fun sessionRequestComplete(
        sessionRequest: Wallet.Model.SessionRequest,
        hashedTxResult: TxResult.HashedTxResult
    )
    suspend fun sessionRequestFailed(sessionRequest: Wallet.Model.SessionRequest)

    suspend fun ethSign(sessionRequest: Wallet.Model.SessionRequest)
    suspend fun ethSend(sessionRequest: Wallet.Model.SessionRequest, method: String)

    companion object {
        const val WC_METHOD_ETH_SEND_TRANSACTION = "eth_sendTransaction"
        const val WC_METHOD_ETH_SIGN_TRANSACTION = "eth_signTransaction"
        const val WC_METHOD_ETH_SIGN = "eth_sign"
        const val WC_METHOD_ETH_SIGN_TYPED_DATA = "eth_signTypedData"
        const val WC_METHOD_ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
        const val WC_METHOD_PERSONAL_SIGN = "personal_sign"

        const val EVM_CHAIN_ROOT = "eip155"
        const val EVM_CHAIN_ROOT_PREFIX = "$EVM_CHAIN_ROOT:"
    }
}

interface WalletConnectV2UrlValidator {
    fun validateURI(uri: String): Boolean
}
