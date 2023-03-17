package com.blockchain.api.services

import com.blockchain.api.ethereum.evm.BalancesRequest
import com.blockchain.api.ethereum.evm.EvmApi
import com.blockchain.api.ethereum.evm.FeeLevel
import com.blockchain.api.ethereum.evm.PushTransactionRequest
import com.blockchain.api.ethereum.evm.TransactionHistoryRequest
import com.blockchain.outcome.map
import java.math.BigInteger

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

    suspend fun getFeeLevels(assetTicker: String) = evmApi.getFees(assetTicker).map {
        EvmTransactionFees(
            mapOf(
                FeeLevel.LOW to it.LOW?.toBigInteger(),
                FeeLevel.NORMAL to it.NORMAL?.toBigInteger(),
                FeeLevel.HIGH to it.HIGH?.toBigInteger(),
            ),
            gasLimit = it.gasLimit?.toBigInteger() ?: BigInteger.ZERO,
            gasLimitContract = it.gasLimitContract?.toBigInteger() ?: BigInteger.ZERO
        )
    }

    suspend fun pushTransaction(rawTransaction: String, networkCurrency: String) = evmApi.pushTransaction(
        request = PushTransactionRequest(
            rawTransaction = rawTransaction,
            currency = networkCurrency,
            apiCode = apiCode
        )
    )

    companion object {
        const val NATIVE_IDENTIFIER = "native"
    }
}

data class EvmTransactionFees(
    val feeLevels: Map<FeeLevel, BigInteger?>,
    val gasLimit: BigInteger,
    val gasLimitContract: BigInteger,
)
