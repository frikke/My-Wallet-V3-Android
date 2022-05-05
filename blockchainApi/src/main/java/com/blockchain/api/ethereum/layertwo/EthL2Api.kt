package com.blockchain.api.ethereum.layertwo

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface EthL2Api {
    @POST("/currency/evm/balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<ApiError, BalancesResponse>

    @POST("/currency/evm/txHistory")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<ApiError, TransactionHistoryResponse>

    @POST("/currency/evm/pushTx")
    suspend fun pushTransaction(@Body request: PushTransactionRequest): Outcome<ApiError, PushTransactionResponse>
}
