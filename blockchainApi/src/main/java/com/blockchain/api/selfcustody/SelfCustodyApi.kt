package com.blockchain.api.selfcustody

import com.blockchain.api.adapters.ApiException
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface SelfCustodyApi {

    @POST("auth")
    suspend fun authenticate(@Body request: AuthRequest): Outcome<ApiException, CommonResponse>

    @POST("subscribe")
    suspend fun subscribe(@Body request: AddSubscriptionRequest): Outcome<ApiException, CommonResponse>

    @POST("unsubscribe")
    suspend fun unsubscribe(@Body request: RemoveSubscriptionRequest): Outcome<ApiException, CommonResponse>

    @POST("subscriptions")
    suspend fun getSubscriptions(
        @Body request: GetSubscriptionsRequest
    ): Outcome<ApiException, GetSubscriptionsResponse>

    @POST("balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<ApiException, BalancesResponse>

    @POST("addresses")
    suspend fun getAddresses(@Body request: AddressesRequest): Outcome<ApiException, AddressesResponse>

    @POST("tx-history")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest

    ): Outcome<ApiException, TransactionHistoryResponse>

    @POST("buildTx")
    suspend fun buildTransaction(@Body request: BuildTxRequest): Outcome<ApiException, BuildTxResponse>

    @POST("pushTx")
    suspend fun pushTransaction(@Body request: PushTxRequest): Outcome<ApiException, PushTxResponse>
}
