package com.blockchain.api.selfcustody

import com.blockchain.api.selfcustody.activity.ActivityResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface SelfCustodyApi {

    @POST("auth")
    suspend fun authenticate(@Body request: AuthRequest): Outcome<Exception, CommonResponse>

    @POST("subscribe")
    suspend fun subscribe(@Body request: AddSubscriptionRequest): Outcome<Exception, CommonResponse>

    @POST("unsubscribe")
    suspend fun unsubscribe(@Body request: RemoveSubscriptionRequest): Outcome<Exception, CommonResponse>

    @POST("subscriptions")
    suspend fun getSubscriptions(
        @Body request: GetSubscriptionsRequest
    ): Outcome<Exception, GetSubscriptionsResponse>

    @POST("balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<Exception, BalancesResponse>

    @POST("addresses")
    suspend fun getAddresses(@Body request: AddressesRequest): Outcome<Exception, AddressesResponse>

    @POST("tx-history")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<Exception, TransactionHistoryResponse>

    @POST("activity")
    suspend fun getActivity(@Body request: ActivityRequest): Outcome<Exception, ActivityResponse>

    @POST("buildTx")
    suspend fun buildTransaction(@Body request: BuildTxRequest): Outcome<Exception, BuildTxResponse>

    @POST("pushTx")
    suspend fun pushTransaction(@Body request: PushTxRequest): Outcome<Exception, PushTxResponse>
}
