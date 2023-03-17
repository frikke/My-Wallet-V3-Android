package com.blockchain.api.ethereum.evm

import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EvmApi {
    @POST("/currency/evm/balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<Exception, BalancesResponse>

    @POST("/currency/evm/txHistory")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<Exception, TransactionHistoryResponse>

    @GET("/currency/evm/fees/{assetTicker}")
    suspend fun getFees(@Path("assetTicker") ticker: String): Outcome<Exception, FeesResponse>

    @POST("/currency/evm/pushTx")
    suspend fun pushTransaction(@Body request: PushTransactionRequest): Outcome<Exception, PushTransactionResponse>
}
