package com.blockchain.api.blockchainCard

import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocsFormDto
import com.blockchain.api.blockchainCard.data.BlockchainCardLegalDocumentDto
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
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
    ): Outcome<Exception, List<ProductDto>>

    @GET("card-issuing/cards")
    suspend fun getCards(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
    ): Outcome<Exception, List<CardDto>>

    @POST("card-issuing/cards")
    suspend fun createCard(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body cardCreationRequest: CardCreationRequestBodyDto
    ): Outcome<Exception, CardDto>

    @DELETE("card-issuing/cards/{cardId}")
    suspend fun deleteCard(
        @Path("cardId") cardId: String,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Outcome<Exception, CardDto>

    @POST("card-issuing/cards/{cardId}/marqeta-card-widget-token")
    suspend fun getCardWidgetToken(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardWidgetTokenDto>

    @GET("card-issuing/cards/{cardId}/eligible-accounts")
    suspend fun getEligibleAccounts(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
    ): Outcome<Exception, List<CardAccountDto>>

    @PUT("card-issuing/cards/{cardId}/account")
    suspend fun linkCardAccount(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
        @Body cardAccountLinkDto: CardAccountLinkDto
    ): Outcome<Exception, CardAccountLinkDto>

    @GET("card-issuing/cards/{cardId}/account")
    suspend fun getCardLinkedAccount(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardAccountLinkDto>

    @PUT("card-issuing/cards/{cardId}/lock")
    suspend fun lockCard(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @PUT("card-issuing/cards/{cardId}/unlock")
    suspend fun unlockCard(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @GET("card-issuing/residential-address")
    suspend fun getResidentialAddress(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
    ): Outcome<Exception, ResidentialAddressRequestDto>

    @PUT("card-issuing/residential-address")
    suspend fun updateResidentialAddress(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body residentialAddress: ResidentialAddressUpdateDto
    ): Outcome<Exception, ResidentialAddressRequestDto>

    @GET("card-issuing/transactions")
    suspend fun getTransactions(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("cardId") cardId: String?,
        @Query("types") types: List<String>?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("toId") toId: String?,
        @Query("fromId") fromId: String?,
        @Query("limit") limit: Int?
    ): Outcome<Exception, List<BlockchainCardTransactionDto>>

    @GET("card-issuing/legal")
    suspend fun getLegalDocuments(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
    ): Outcome<Exception, List<BlockchainCardLegalDocumentDto>>

    @PUT("card-issuing/legal")
    suspend fun acceptLegalDocuments(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body acceptedDocumentsForm: BlockchainCardAcceptedDocsFormDto
    ): Outcome<Exception, List<BlockchainCardLegalDocumentDto>>
}
