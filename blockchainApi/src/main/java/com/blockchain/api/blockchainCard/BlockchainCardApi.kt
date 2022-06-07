package com.blockchain.api.blockchainCard

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.blockchainCard.data.CardAccount
import com.blockchain.api.blockchainCard.data.CardAccountLinkRequest
import com.blockchain.api.blockchainCard.data.CardAccountLinkResponse
import com.blockchain.api.blockchainCard.data.CardCreationRequestBody
import com.blockchain.api.blockchainCard.data.CardLinkedAccountResponse
import com.blockchain.api.blockchainCard.data.CardWidgetTokenResponse
import com.blockchain.api.blockchainCard.data.CardsResponse
import com.blockchain.api.blockchainCard.data.ProductsResponse
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface BlockchainCardApi {

    @GET("card-issuing/products")
    suspend fun getProducts(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, List<ProductsResponse>>

    @GET("card-issuing/cards")
    suspend fun getCards(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, List<CardsResponse>>

    @POST("card-issuing/cards")
    suspend fun createCard(
        @Header("authorization") authorization: String,
        @Body cardCreationRequest: CardCreationRequestBody
    ): Outcome<ApiError, CardsResponse>

    @DELETE("card-issuing/cards/{cardId}")
    suspend fun deleteCard(
        @Path("cardId") cardId: String,
        @Header("authorization") authorization: String
    ): Outcome<ApiError, CardsResponse>

    @POST("card-issuing/cards/{cardId}/marqeta-card-widget-token")
    suspend fun getCardWidgetToken(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardWidgetTokenResponse>

    @GET("card-issuing/cards/{cardId}/eligible-accounts")
    suspend fun getEligibleAccounts(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, List<CardAccount>>

    @PUT("card-issuing/cards/{cardId}/account")
    suspend fun linkCardAccount(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Body cardAccountLinkRequest: CardAccountLinkRequest
    ): Outcome<ApiError, CardAccountLinkResponse>

    @GET("card-issuing/cards/{cardId}/account")
    suspend fun getCardLinkedAccount(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardLinkedAccountResponse>

    @PUT("card-issuing/cards/{cardId}/lock")
    suspend fun lockCard(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardsResponse>

    @PUT("card-issuing/cards/{cardId}/unlock")
    suspend fun unlockCard(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardsResponse>
}
