package com.blockchain.api.paymentmethods

import com.blockchain.api.brokerage.data.DepositTermsResponse
import com.blockchain.api.paymentmethods.models.ActivateCardResponse
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.AddNewCardResponse
import com.blockchain.api.paymentmethods.models.AliasInfoRequestBody
import com.blockchain.api.paymentmethods.models.AliasInfoResponse
import com.blockchain.api.paymentmethods.models.CardRejectionStateResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.paymentmethods.models.DepositTermsRequestBody
import com.blockchain.api.paymentmethods.models.GooglePayResponse
import com.blockchain.api.paymentmethods.models.LinkWithAliasRequestBody
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.api.payments.data.BankInfoResponse
import com.blockchain.api.payments.data.BankTransferChargeResponse
import com.blockchain.api.payments.data.BankTransferPaymentBody
import com.blockchain.api.payments.data.BankTransferPaymentResponse
import com.blockchain.api.payments.data.CreateLinkBankRequestBody
import com.blockchain.api.payments.data.CreateLinkBankResponse
import com.blockchain.api.payments.data.LinkPlaidAccountBody
import com.blockchain.api.payments.data.LinkedBankTransferResponse
import com.blockchain.api.payments.data.OpenBankingTokenBody
import com.blockchain.api.payments.data.RefreshPlaidRequestBody
import com.blockchain.api.payments.data.RefreshPlaidResponse
import com.blockchain.api.payments.data.SettlementBody
import com.blockchain.api.payments.data.SettlementResponse
import com.blockchain.api.payments.data.UpdateProviderAccountBody
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PaymentMethodsApi {

    @GET("eligible/payment-methods")
    fun getAvailablePaymentMethodsTypes(
        @Query("currency") currency: String,
        @Query("tier") tier: Int?,
        @Query("eligibleOnly") eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>>

    @GET("payments/cards")
    fun getCards(
        @Query("cardProvider") cardProvidersSupported: Boolean
    ): Single<List<CardResponse>>

    @POST("payments/cards")
    fun addNewCard(
        @Body addNewCardBody: AddNewCardBodyRequest,
        @Query("localisedError") localisedError: String?
    ): Single<AddNewCardResponse>

    @POST("payments/cards/{cardId}/activate")
    fun activateCard(
        @Path("cardId") cardId: String,
        @Body attributes: SimpleBuyConfirmationAttributes
    ): Single<ActivateCardResponse>

    @GET("payments/cards/{cardId}")
    fun getCardDetails(
        @Path("cardId") cardId: String
    ): Single<CardResponse>

    @DELETE("payments/cards/{cardId}")
    fun deleteCard(
        @Path("cardId") cardId: String
    ): Completable

    @GET("payments/banking-info")
    fun getBanks(): Single<List<BankInfoResponse>>

    @GET("payments/banktransfer/{id}")
    fun getLinkedBank(
        @Path("id") id: String,
        @Query("localisedError") localisedError: String?
    ): Single<LinkedBankTransferResponse>

    @DELETE("payments/banks/{id}")
    fun removeBeneficiary(
        @Path("id") id: String
    ): Completable

    @DELETE("payments/banktransfer/{id}")
    fun removeLinkedBank(
        @Path("id") id: String,
        @Query("localisedError") localisedError: String?
    ): Completable

    @POST("payments/banktransfer")
    fun linkBank(
        @Body body: CreateLinkBankRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<CreateLinkBankResponse>

    @POST("payments/banktransfer/{id}/update")
    fun updateProviderAccount(
        @Path("id") id: String,
        @Body body: UpdateProviderAccountBody,
        @Query("localisedError") localisedError: String?
    ): Completable

    @POST("payments/banktransfer/{id}/update")
    fun linkPlaidAccount(
        @Path("id") id: String,
        @Body body: LinkPlaidAccountBody
    ): Completable

    @POST("payments/banktransfer/{id}/update")
    fun checkSettlement(
        @Path("id") id: String,
        @Body body: SettlementBody,
        @Query("localisedError") localisedError: String?
    ): Single<SettlementResponse>

    @PUT("payments/deposit/terms")
    suspend fun getDepositTerms(
        @Body body: DepositTermsRequestBody,
        @Query("localisedError") localisedError: String?
    ): Outcome<Exception, DepositTermsResponse>

    @POST("payments/banktransfer/{id}/payment")
    fun startBankTransferPayment(
        @Path("id") id: String,
        @Body body: BankTransferPaymentBody,
        @Query("localisedError") localisedError: String?
    ): Single<BankTransferPaymentResponse>

    @POST("payments/banktransfer/{id}/refresh")
    fun refreshPlaidAccount(
        @Path("id") id: String,
        @Body body: RefreshPlaidRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<RefreshPlaidResponse>

    @POST
    fun updateOpenBankingToken(
        @Url url: String,
        @Body body: OpenBankingTokenBody
    ): Completable

    @GET("payments/payment/{paymentId}")
    fun getBankTransferCharge(
        @Path("paymentId") paymentId: String,
        @Query("localisedError") localisedError: String?
    ): Single<BankTransferChargeResponse>

    @GET("payments/google-pay/info")
    fun getGooglePayInfo(
        @Query("currency") currency: String
    ): Single<GooglePayResponse>

    @POST("payments/bind/beneficiary")
    suspend fun getBeneficiaryInfo(
        @Body body: AliasInfoRequestBody,
        @Query("localisedError") localisedError: String?
    ): Outcome<Exception, AliasInfoResponse>

    @PUT("payments/bind/beneficiary")
    suspend fun activateBeneficiary(
        @Body body: LinkWithAliasRequestBody
    ): Outcome<Exception, Unit>

    @GET("payments/cards/success-rate")
    suspend fun checkNewCardRejectionState(
        @Query("bin") binNumber: String
    ): Outcome<Exception, CardRejectionStateResponse>
}
