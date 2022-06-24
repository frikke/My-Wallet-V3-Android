package com.blockchain.api.selfcustody

import com.blockchain.api.adapters.ApiError
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.POST

interface SelfCustodyApi {

    @POST("/wallet-pubkey/auth")
    suspend fun authenticate(@Body request: AuthRequest): Outcome<ApiError, CommonResponse>

    @POST("/wallet-pubkey/subscribe")
    suspend fun subscribe(@Body request: AddSubscriptionRequest): Outcome<ApiError, CommonResponse>

    @POST("/wallet-pubkey/unsubscribe")
    suspend fun unsubscribe(@Body request: RemoveSubscriptionRequest): Outcome<ApiError, CommonResponse>

    @POST("/wallet-pubkey/subscriptions")
    suspend fun getSubscriptions(@Body request: GetSubscriptionsRequest): Outcome<ApiError, GetSubscriptionsResponse>

    @POST("/wallet-pubkey/balance")
    suspend fun getBalances(@Body request: BalancesRequest): Outcome<ApiError, BalancesResponse>

    @POST("/wallet-pubkey/addresses")
    suspend fun getAddresses(@Body request: AddressesRequest): Outcome<ApiError, AddressesResponse>

    @POST("/wallet-pubkey/tx-history")
    suspend fun getTransactionHistory(
        @Body request: TransactionHistoryRequest
    ): Outcome<ApiError, TransactionHistoryResponse>
}
