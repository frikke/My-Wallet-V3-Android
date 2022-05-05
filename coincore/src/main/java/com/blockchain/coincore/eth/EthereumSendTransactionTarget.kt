package com.blockchain.coincore.eth

import com.blockchain.coincore.TxResult
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.ethereum.util.EthUtils
import io.reactivex.rxjava3.core.Completable
import java.math.BigInteger

class EthereumSendTransactionTarget(
    val dAppAddress: String,
    val dAppName: String,
    val dAppLogoURL: String,
    private val transaction: EthereumJsonRpcTransaction,
    val method: Method,
    override val onTxCompleted: (TxResult) -> Completable,
    override val onTxCancelled: () -> Completable
) : WalletConnectTarget {
    override val asset: AssetInfo
        get() = CryptoCurrency.ETHER
    override val label: String
        get() = dAppName
    override val isDomain: Boolean = false
    override val address: String
        get() = transaction.to.orEmpty()
    val data: String
        get() = transaction.data
    val nonce: BigInteger?
        get() = transaction.nonce?.let {
            EthUtils.convertHexToBigInteger(it)
        }

    override val amount: Money
        get() = transaction.value?.let {
            Money.fromMinor(
                asset,
                EthUtils.convertHexToBigInteger(it)
            )
        } ?: Money.zero(asset)

    val transactionSource: String
        get() = transaction.from

    val gasPrice: BigInteger?
        get() = transaction.gasPrice?.let {
            EthUtils.convertHexToBigInteger(it)
        }

    val gasLimit: BigInteger?
        get() = transaction.gas?.let {
            EthUtils.convertHexToBigInteger(it)
        }

    enum class Method {
        SEND, SIGN
    }
}
