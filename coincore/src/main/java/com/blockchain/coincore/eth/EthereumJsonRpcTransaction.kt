package com.blockchain.coincore.eth

/**
 *  A representation of a  transaction that can be passed to Ethereum JSON RPC methods.
 *  All parameters are hexadecimal String values.
 */
data class EthereumJsonRpcTransaction(
    // from: DATA, 20 Bytes - The address the transaction is send from.
    val from: String,
    // to: DATA, 20 Bytes - (optional when creating new contract) The address the transaction is directed to.
    val to: String?,
    // data: DATA - The compiled code of a contract OR the hash of the invoked method signature and encoded parameters.
    val data: String,
    // gas: QUANTITY - (optional, default: 90000) Integer of the gas provided for the transaction execution. It will return unused gas.
    val gas: String?,
    // gasPrice: QUANTITY - (optional, default: To-Be-Determined) Integer of the gasPrice used for each paid gas
    val gasPrice: String?,
    // value: QUANTITY - (optional) Integer of the value sent with this transaction
    val value: String?,
    // nonce: QUANTITY - (optional) Integer of a nonce. This allows to overwrite your own pending transactions that use the same nonce.
    val nonce: String?
)
