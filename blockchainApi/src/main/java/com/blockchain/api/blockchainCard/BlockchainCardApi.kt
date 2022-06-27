package com.blockchain.api.blockchainCard

import com.blockchain.api.adapters.ApiError
import com.blockchain.api.blockchainCard.data.BlockchainCardTransactionDto
import com.blockchain.api.blockchainCard.data.CardAccountDto
import com.blockchain.api.blockchainCard.data.CardAccountLinkDto
import com.blockchain.api.blockchainCard.data.CardCreationRequestBodyDto
import com.blockchain.api.blockchainCard.data.CardDto
import com.blockchain.api.blockchainCard.data.CardWidgetTokenDto
import com.blockchain.api.blockchainCard.data.ProductDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressRequestDto
import com.blockchain.api.blockchainCard.data.ResidentialAddressUpdateDto
import com.blockchain.outcome.Outcome
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface BlockchainCardApi {

    @GET("card-issuing/products")
    suspend fun getProducts(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, List<ProductDto>>

    @GET("card-issuing/cards")
    suspend fun getCards(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, List<CardDto>>

    @POST("card-issuing/cards")
    suspend fun createCard(
        @Header("authorization") authorization: String,
        @Body cardCreationRequest: CardCreationRequestBodyDto
    ): Outcome<ApiError, CardDto>

    @DELETE("card-issuing/cards/{cardId}")
    suspend fun deleteCard(
        @Path("cardId") cardId: String,
        @Header("authorization") authorization: String
    ): Outcome<ApiError, CardDto>

    @POST("card-issuing/cards/{cardId}/marqeta-card-widget-token")
    suspend fun getCardWidgetToken(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardWidgetTokenDto>

    @GET("card-issuing/cards/{cardId}/eligible-accounts")
    suspend fun getEligibleAccounts(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, List<CardAccountDto>>

    @PUT("card-issuing/cards/{cardId}/account")
    suspend fun linkCardAccount(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
        @Body cardAccountLinkDto: CardAccountLinkDto
    ): Outcome<ApiError, CardAccountLinkDto>

    @GET("card-issuing/cards/{cardId}/account")
    suspend fun getCardLinkedAccount(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardAccountLinkDto>

    @PUT("card-issuing/cards/{cardId}/lock")
    suspend fun lockCard(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardDto>

    @PUT("card-issuing/cards/{cardId}/unlock")
    suspend fun unlockCard(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String,
    ): Outcome<ApiError, CardDto>

    @GET("card-issuing/residential-address")
    suspend fun getResidentialAddress(
        @Header("authorization") authorization: String,
    ): Outcome<ApiError, ResidentialAddressRequestDto>

    @PUT("card-issuing/residential-address")
    suspend fun updateResidentialAddress(
        @Header("authorization") authorization: String,
        @Body residentialAddress: ResidentialAddressUpdateDto
    ): Outcome<ApiError, ResidentialAddressRequestDto>

    @GET("card-issuing/transactions")
    suspend fun getTransactions(
        @Header("authorization") authorization: String,
        @Query("cardId") cardId: String?,
        @Query("types") types: List<String>?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("toId") toId: String?,
        @Query("fromId") fromId: String?,
        @Query("limit") limit: Int?
    ): Outcome<ApiError, List<BlockchainCardTransactionDto>>
}
