package com.blockchain.api.blockchainCard

import com.blockchain.api.blockchainCard.data.BlockchainCardAcceptedDocsFormDto
import com.blockchain.api.blockchainCard.data.BlockchainCardActivationUrlResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardGoogleWalletProvisionResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycStatusDto
import com.blockchain.api.blockchainCard.data.BlockchainCardKycUpdateRequestDto
import com.blockchain.api.blockchainCard.data.BlockchainCardLegalDocumentDto
import com.blockchain.api.blockchainCard.data.BlockchainCardOrderStateResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardSetPinURLResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardStatementUrlResponseDto
import com.blockchain.api.blockchainCard.data.BlockchainCardStatementsResponseDto
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
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface BlockchainCardApi {

    @GET("card-issuing/products")
    suspend fun getProducts(): Outcome<Exception, List<ProductDto>>

    @GET("card-issuing/cards")
    suspend fun getCards(): Outcome<Exception, List<CardDto>>

    @POST("card-issuing/cards")
    suspend fun createCard(
        @Body cardCreationRequest: CardCreationRequestBodyDto
    ): Outcome<Exception, CardDto>

    @GET("card-issuing/cards/{cardId}")
    suspend fun getCard(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @DELETE("card-issuing/cards/{cardId}")
    suspend fun deleteCard(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @POST("card-issuing/cards/{cardId}/marqeta-card-widget-token")
    suspend fun getCardWidgetToken(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardWidgetTokenDto>

    @GET("card-issuing/cards/{cardId}/eligible-accounts")
    suspend fun getEligibleAccounts(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, List<CardAccountDto>>

    @PUT("card-issuing/cards/{cardId}/account")
    suspend fun linkCardAccount(
        @Path("cardId") cardId: String,
        @Body cardAccountLinkDto: CardAccountLinkDto
    ): Outcome<Exception, CardAccountLinkDto>

    @GET("card-issuing/cards/{cardId}/account")
    suspend fun getCardLinkedAccount(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardAccountLinkDto>

    @PUT("card-issuing/cards/{cardId}/lock")
    suspend fun lockCard(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @PUT("card-issuing/cards/{cardId}/unlock")
    suspend fun unlockCard(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, CardDto>

    @GET("card-issuing/residential-address")
    suspend fun getResidentialAddress(): Outcome<Exception, ResidentialAddressRequestDto>

    @PUT("card-issuing/residential-address")
    suspend fun updateResidentialAddress(
        @Body residentialAddress: ResidentialAddressUpdateDto
    ): Outcome<Exception, ResidentialAddressRequestDto>

    @GET("card-issuing/transactions")
    suspend fun getTransactions(
        @Query("cardId") cardId: String?,
        @Query("types") types: List<String>?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("toId") toId: String?,
        @Query("fromId") fromId: String?,
        @Query("limit") limit: Int?
    ): Outcome<Exception, List<BlockchainCardTransactionDto>>

    @GET("card-issuing/legal")
    suspend fun getLegalDocuments(): Outcome<Exception, List<BlockchainCardLegalDocumentDto>>

    @PUT("card-issuing/legal")
    suspend fun acceptLegalDocuments(
        @Body acceptedDocumentsForm: BlockchainCardAcceptedDocsFormDto
    ): Outcome<Exception, List<BlockchainCardLegalDocumentDto>>

    @POST("card-issuing/cards/{cardId}/digital-wallets/google-wallet")
    suspend fun provisionGoogleWalletCard(
        @Path("cardId") cardId: String,
        @Body provisionRequest: BlockchainCardGoogleWalletProvisionRequestDto
    ): Outcome<Exception, BlockchainCardGoogleWalletProvisionResponseDto>

    @GET("card-issuing/cards/{cardId}/fulfillment")
    suspend fun getCardOrderState(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, BlockchainCardOrderStateResponseDto>

    @GET("card-issuing/cards/activate-widget-url")
    suspend fun getCardActivationUrl(): Outcome<Exception, BlockchainCardActivationUrlResponseDto>

    @GET("card-issuing/statements")
    suspend fun getCardStatements(): Outcome<Exception, List<BlockchainCardStatementsResponseDto>>

    @GET("card-issuing/statements/{statementId}")
    suspend fun getCardStatementUrl(
        @Path("statementId") statementId: String,
    ): Outcome<Exception, BlockchainCardStatementUrlResponseDto>

    @GET("card-issuing/kyc")
    suspend fun getKycStatus(): Outcome<Exception, BlockchainCardKycStatusDto>

    @POST("card-issuing/kyc")
    suspend fun updateKyc(
        @Body kycUpdateRequest: BlockchainCardKycUpdateRequestDto
    ): Outcome<Exception, BlockchainCardKycStatusDto>

    @GET("card-issuing/cards/{cardId}/pin-widget-url")
    suspend fun getSetPinUrl(
        @Path("cardId") cardId: String,
    ): Outcome<Exception, BlockchainCardSetPinURLResponseDto>
}
