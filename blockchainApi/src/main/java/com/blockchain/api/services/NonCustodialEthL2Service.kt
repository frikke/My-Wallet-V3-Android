package com.blockchain.api.services

import com.blockchain.api.ethereum.layertwo.BalancesRequest
import com.blockchain.api.ethereum.layertwo.EthL2Api
import com.blockchain.api.ethereum.layertwo.PushTransactionRequest
import com.blockchain.api.ethereum.layertwo.TransactionHistoryRequest

class NonCustodialEthL2Service(
    private val l2Api: EthL2Api,
    private val apiCode: String
) {
    suspend fun getBalances(address: String, network: String) = l2Api.getBalances(
        request = BalancesRequest(
            addresses = listOf(address),
            apiCode = apiCode,
            network = network
        )
    )

    suspend fun getTransactionHistory(address: String, contractAddress: String, parentChain: String) =
        l2Api.getTransactionHistory(
            request = TransactionHistoryRequest(
                addresses = listOf(address),
                apiCode = apiCode,
                contractAddress = contractAddress,
                network = parentChain
            )
        )

    suspend fun pushTransaction(rawTransaction: String, network: String) = l2Api.pushTransaction(
        request = PushTransactionRequest(
            rawTransaction = rawTransaction,
            networkName = network,
            apiCode = apiCode
        )
    )
}
