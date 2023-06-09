package com.blockchain.walletconnect.domain

import android.app.Application
import com.blockchain.coincore.TxResult
import com.walletconnect.web3.wallet.client.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface WalletConnectV2Service {

    val walletEvents: SharedFlow<Wallet.Model>
    val userEvents: SharedFlow<WalletConnectUserEvent>

    fun initWalletConnect(application: Application, projectId: String, relayUrl: String)

    suspend fun pair(pairingUrl: String)

    fun buildApprovedSessionNamespaces(
        sessionProposal: Wallet.Model.SessionProposal
    ): Flow<Map<String, Wallet.Model.Namespace.Session>>
    fun approveLastSession()
    fun getSessionProposalState(): Flow<WalletConnectSessionProposalState?>
    suspend fun getSessions(): List<WalletConnectSession>
    fun getSessionsFlow(): Flow<List<WalletConnectSession>>

    suspend fun getSession(sessionId: String): WalletConnectSession?
    suspend fun disconnectSession(sessionTopic: String)
    suspend fun disconnectAllSessions()

    suspend fun getSessionProposal(sessionId: String): Wallet.Model.SessionProposal?
    fun clearSessionProposals()

    suspend fun getAuthRequest(authId: String): Wallet.Model.PendingAuthRequest?

    suspend fun buildAuthSigningPayload(authId: String): Flow<WalletConnectAuthSigningPayload>
    suspend fun approveAuthRequest(authSigningPayload: WalletConnectAuthSigningPayload)
    suspend fun rejectAuthRequest(authId: String)

    fun Wallet.Model.PendingAuthRequest.getAuthMessage(issuer: String): String
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

        const val ISS_DID_PREFIX = "did:pkh:"
    }
}

interface WalletConnectV2UrlValidator {
    fun validateURI(uri: String): Boolean
}

enum class WalletConnectSessionProposalState {
    APPROVED,
    REJECTED
}

data class WalletConnectAuthSigningPayload(
    val authId: String,
    val authMessage: String,
    val issuer: String,
    val domain: String,
)
