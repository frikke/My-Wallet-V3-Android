package com.blockchain.walletconnect.data

import com.blockchain.coincore.TxResult
import com.blockchain.coincore.eth.EthSignMessage
import com.blockchain.coincore.eth.EthereumJsonRpcTransaction
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.walletconnect.domain.EthRequestSign
import com.blockchain.walletconnect.domain.EthSendTransactionRequest
import com.blockchain.walletconnect.domain.WalletConnectEthAccountProvider
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletConnectUserEvent
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SEND_TRANSACTION
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN_TRANSACTION
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_ETH_SIGN_TYPED_DATA
import com.blockchain.walletconnect.domain.WalletConnectV2Service.Companion.WC_METHOD_PERSONAL_SIGN
import com.blockchain.walletconnect.ui.networks.ETH_CHAIN_ID
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.walletconnect.web3.wallet.client.Wallet
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SignRequestHandler(
    private val accountProvider: WalletConnectEthAccountProvider
) : EthRequestSign, EthSendTransactionRequest {

    override fun onEthSign(
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable
    ): Single<WalletConnectUserEvent.SignMessage> {
        return accountProvider.account().map { account ->
            val target = EthereumSignMessageTarget(
                dAppAddress = session.dAppInfo.peerMeta.url,
                dAppName = session.dAppInfo.peerMeta.name,
                dAppLogoUrl = session.dAppInfo.peerMeta.uiIcon(),
                message = message.toEthSignedMessage(),
                onTxCompleted = onTxCompleted,
                onTxCancelled = onTxCancelled
            )
            WalletConnectUserEvent.SignMessage(
                source = account,
                target = target
            )
        }
    }

    override fun onEthSignV2(
        sessionRequest: Wallet.Model.SessionRequest,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable
    ): Flow<WalletConnectUserEvent.SignMessage> =
        sessionRequest.chainId?.let { chainId ->
            val accountFlow = if (chainId.substringAfter(":").toInt() == ETH_CHAIN_ID)
                accountProvider.ethAccountFlow()
            else
                accountProvider.account(chainId)

            accountFlow.map { account ->
                sessionRequest.peerMetaData?.let { dappMetaData ->
                    val target = EthereumSignMessageTarget(
                        dAppAddress = dappMetaData.url,
                        dAppName = dappMetaData.name,
                        dAppLogoUrl = dappMetaData.icons.firstOrNull().orEmpty(),
                        message = sessionRequest.request.toEthSignedMessage(),
                        onTxCompleted = onTxCompleted,
                        onTxCancelled = onTxCancelled,
                        currency = account.currency
                    )
                    WalletConnectUserEvent.SignMessage(
                        source = account,
                        target = target
                    )
                } ?: throw IllegalStateException("No peer metadata found")
            }
        } ?: run {
            emptyFlow()
        }

    override fun onSendTransaction(
        transaction: WCEthereumTransaction,
        session: WalletConnectSession,
        method: EthereumSendTransactionTarget.Method,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable
    ): Single<WalletConnectUserEvent.SendTransaction> {
        return accountProvider.account().map { account ->
            val target = EthereumSendTransactionTarget(
                dAppAddress = session.dAppInfo.peerMeta.url,
                dAppName = session.dAppInfo.peerMeta.name,
                dAppLogoURL = session.dAppInfo.peerMeta.uiIcon(),
                transaction = EthereumJsonRpcTransaction(
                    from = transaction.from,
                    to = transaction.to,
                    gas = transaction.gas,
                    gasPrice = transaction.gasPrice,
                    value = transaction.value,
                    nonce = transaction.nonce,
                    data = transaction.data
                ),
                onTxCancelled = onTxCancelled,
                onTxCompleted = onTxCompleted,
                method = method
            )
            WalletConnectUserEvent.SendTransaction(
                source = account,
                target = target
            )
        }
    }

    override fun onSendTransactionV2(
        sessionRequest: Wallet.Model.SessionRequest,
        method: String,
        onTxCompleted: (TxResult) -> Completable,
        onTxCancelled: () -> Completable
    ): Flow<WalletConnectUserEvent> =
        sessionRequest.chainId?.let { chainId ->
            val accountFlow = if (chainId.substringAfter(":").toInt() == ETH_CHAIN_ID)
                accountProvider.ethAccountFlow()
            else
                accountProvider.account(chainId)

            accountFlow.map { account ->
                sessionRequest.peerMetaData?.let { dappMetaData ->
                    val target = EthereumSendTransactionTarget(
                        dAppAddress = dappMetaData.url,
                        dAppName = dappMetaData.name,
                        dAppLogoURL = dappMetaData.icons.firstOrNull().orEmpty(),
                        transaction = sessionRequest.request.toEthTransaction(),
                        onTxCancelled = onTxCancelled,
                        onTxCompleted = onTxCompleted,
                        method = method.toEthereumSendTransactionMethod(),
                        asset = account.currency as AssetInfo
                    )

                    when (method) {
                        WC_METHOD_ETH_SEND_TRANSACTION -> WalletConnectUserEvent.SendTransaction(
                            source = account,
                            target = target
                        )

                        WC_METHOD_ETH_SIGN_TRANSACTION -> WalletConnectUserEvent.SignTransaction(
                            source = account,
                            target = target
                        )

                        else -> throw IllegalStateException("Invalid Wallet Connect v2 method: $method")
                    }
                } ?: throw IllegalStateException("No peer metadata found")
            }
        } ?: run {
            emptyFlow()
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

private fun Wallet.Model.SessionRequest.JSONRPCRequest.toEthSignedMessage(): EthSignMessage {

    val jsonParams = Json.parseToJsonElement(this.params).jsonArray
    // convert JsonArray to List<String>
    val rawMessage = jsonParams.map { it.jsonPrimitive.content }

    return EthSignMessage(
        raw = rawMessage,
        type = when (this.method) {
            WC_METHOD_ETH_SIGN -> EthSignMessage.SignType.MESSAGE
            WC_METHOD_ETH_SIGN_TYPED_DATA -> EthSignMessage.SignType.TYPED_MESSAGE
            WC_METHOD_PERSONAL_SIGN -> EthSignMessage.SignType.PERSONAL_MESSAGE
            else -> EthSignMessage.SignType.MESSAGE
        }
    )
}

private fun String.toEthereumSendTransactionMethod(): EthereumSendTransactionTarget.Method {
    return when (this) {
        WC_METHOD_ETH_SEND_TRANSACTION -> EthereumSendTransactionTarget.Method.SEND
        WC_METHOD_ETH_SIGN_TRANSACTION -> EthereumSendTransactionTarget.Method.SIGN
        else -> throw IllegalStateException("Unknown method $this")
    }
}

private fun Wallet.Model.SessionRequest.JSONRPCRequest.toEthTransaction(): EthereumJsonRpcTransaction {
    val jsonParams = Json.parseToJsonElement(this.params).jsonArray
    val transactionParams = jsonParams[0].jsonObject

    val from = transactionParams["from"]?.jsonPrimitive?.content ?: ""
    val to = transactionParams["to"]?.jsonPrimitive?.content ?: ""
    val data = transactionParams["data"]?.jsonPrimitive?.content ?: ""
    val gas = transactionParams["gas"]?.jsonPrimitive?.content ?: "0x15F90" // default according to WC docs
    val nonce = transactionParams["nonce"]?.jsonPrimitive?.content
    val gasPrice = transactionParams["gasPrice"]?.jsonPrimitive?.content
    val value = transactionParams["value"]?.jsonPrimitive?.content

    return EthereumJsonRpcTransaction(
        from = from,
        to = to,
        gas = gas,
        gasPrice = gasPrice,
        value = value,
        nonce = nonce,
        data = data
    )
}
