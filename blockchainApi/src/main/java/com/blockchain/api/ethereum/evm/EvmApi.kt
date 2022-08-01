package com.blockchain.api.ethereum.evm

import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface EvmApi {
    @POST("/currency/evm/balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<Exception, BalancesResponse>

    @POST("/currency/evm/txHistory")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<Exception, TransactionHistoryResponse>

    @POST("/currency/evm/pushTx")
    suspend fun pushTransaction(@Body request: PushTransactionRequest): Outcome<Exception, PushTransactionResponse>
}
