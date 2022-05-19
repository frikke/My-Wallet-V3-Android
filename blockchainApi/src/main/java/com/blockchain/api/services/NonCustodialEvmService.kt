package com.blockchain.api.services

import com.blockchain.api.ethereum.evm.BalancesRequest
import com.blockchain.api.ethereum.evm.EvmApi
import com.blockchain.api.ethereum.evm.PushTransactionRequest
import com.blockchain.api.ethereum.evm.TransactionHistoryRequest

class NonCustodialEvmService(
    private val evmApi: EvmApi,
    private val apiCode: String
) {
    suspend fun getBalances(address: String, network: String) = evmApi.getBalances(
        request = BalancesRequest(
            addresses = listOf(address),
            apiCode = apiCode,
            network = network
        )
    )

    suspend fun getTransactionHistory(address: String, contractAddress: String?, parentChain: String) =
        evmApi.getTransactionHistory(
            request = TransactionHistoryRequest(
                addresses = listOf(address),
                apiCode = apiCode,
                contractAddress = contractAddress,
                network = parentChain
            )
        )

    suspend fun pushTransaction(rawTransaction: String, network: String) = evmApi.pushTransaction(
        request = PushTransactionRequest(
            rawTransaction = rawTransaction,
            networkName = network,
            apiCode = apiCode
        )
    )

    companion object {
        const val NATIVE_IDENTIFIER = "native"
    }
}
