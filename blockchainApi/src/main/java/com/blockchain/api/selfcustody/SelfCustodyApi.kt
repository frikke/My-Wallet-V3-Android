package com.blockchain.api.selfcustody

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface SelfCustodyApi {

    @POST("auth")
    suspend fun authenticate(@Body request: AuthRequest): Outcome<ApiError, CommonResponse>

    @POST("subscribe")
    suspend fun subscribe(@Body request: AddSubscriptionRequest): Outcome<ApiError, CommonResponse>

    @POST("unsubscribe")
    suspend fun unsubscribe(@Body request: RemoveSubscriptionRequest): Outcome<ApiError, CommonResponse>

    @POST("subscriptions")
    suspend fun getSubscriptions(@Body request: GetSubscriptionsRequest): Outcome<ApiError, GetSubscriptionsResponse>

    @POST("balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<ApiError, BalancesResponse>

    @POST("addresses")
    suspend fun getAddresses(@Body request: AddressesRequest): Outcome<ApiError, AddressesResponse>

    @POST("tx-history")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<ApiError, TransactionHistoryResponse>

    @POST("buildTx")
    suspend fun buildTransaction(@Body request: BuildTxRequest): Outcome<ApiError, BuildTxResponse>

    @POST("pushTx")
    suspend fun pushTransaction(@Body request: PushTxRequest): Outcome<ApiError, PushTxResponse>
}
