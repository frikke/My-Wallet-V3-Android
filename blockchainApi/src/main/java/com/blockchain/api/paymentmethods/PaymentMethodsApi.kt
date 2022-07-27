package com.blockchain.api.paymentmethods

import com.blockchain.api.adapters.ApiException
import com.blockchain.api.paymentmethods.models.ActivateCardResponse
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.AddNewCardResponse
import com.blockchain.api.paymentmethods.models.AliasInfoRequestBody
import com.blockchain.api.paymentmethods.models.AliasInfoResponse
import com.blockchain.api.paymentmethods.models.CardRejectionStateResponse
import com.blockchain.api.paymentmethods.models.CardResponse
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
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PaymentMethodsApi {

    @GET("eligible/payment-methods")
    fun getAvailablePaymentMethodsTypes(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String,
        @Query("tier") tier: Int?,
        @Query("eligibleOnly") eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>>

    @GET("payments/cards")
    fun getCards(
        @Header("authorization") authorization: String,
        @Query("cardProvider") cardProvidersSupported: Boolean
    ): Single<List<CardResponse>>

    @POST("payments/cards")
    fun addNewCard(
        @Header("authorization") authHeader: String,
        @Body addNewCardBody: AddNewCardBodyRequest,
        @Query("localisedError") localisedError: String?
    ): Single<AddNewCardResponse>

    @POST("payments/cards/{cardId}/activate")
    fun activateCard(
        @Header("authorization") authHeader: String,
        @Path("cardId") cardId: String,
        @Body attributes: SimpleBuyConfirmationAttributes
    ): Single<ActivateCardResponse>

    @GET("payments/cards/{cardId}")
    fun getCardDetails(
        @Header("authorization") authorization: String,
        @Path("cardId") cardId: String
    ): Single<CardResponse>

    @DELETE("payments/cards/{cardId}")
    fun deleteCard(
        @Header("authorization") authHeader: String,
        @Path("cardId") cardId: String
    ): Completable

    @GET("payments/banking-info")
    fun getBanks(
        @Header("authorization") authorization: String
    ): Single<List<BankInfoResponse>>

    @GET("payments/banktransfer/{id}")
    fun getLinkedBank(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Query("localisedError") localisedError: String?
    ): Single<LinkedBankTransferResponse>

    @DELETE("payments/banks/{id}")
    fun removeBeneficiary(
        @Header("authorization") authHeader: String,
        @Path("id") id: String
    ): Completable

    @DELETE("payments/banktransfer/{id}")
    fun removeLinkedBank(
        @Header("authorization") authHeader: String,
        @Path("id") id: String,
        @Query("localisedError") localisedError: String?
    ): Completable

    @POST("payments/banktransfer")
    fun linkBank(
        @Header("authorization") authorization: String,
        @Body body: CreateLinkBankRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<CreateLinkBankResponse>

    @POST("payments/banktransfer/{id}/update")
    fun updateProviderAccount(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: UpdateProviderAccountBody,
        @Query("localisedError") localisedError: String?
    ): Completable

    @POST("payments/banktransfer/{id}/update")
    fun linkPlaidAccount(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: LinkPlaidAccountBody
    ): Completable

    @POST("payments/banktransfer/{id}/update")
    fun checkSettlement(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: SettlementBody,
        @Query("localisedError") localisedError: String?
    ): Single<SettlementResponse>

    @POST("payments/banktransfer/{id}/payment")
    fun startBankTransferPayment(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: BankTransferPaymentBody,
        @Query("localisedError") localisedError: String?
    ): Single<BankTransferPaymentResponse>

    @POST("payments/banktransfer/{id}/refresh")
    fun refreshPlaidAccount(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: RefreshPlaidRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<RefreshPlaidResponse>

    @POST
    fun updateOpenBankingToken(
        @Url url: String,
        @Header("authorization") authorization: String,
        @Body body: OpenBankingTokenBody
    ): Completable

    @GET("payments/payment/{paymentId}")
    fun getBankTransferCharge(
        @Header("authorization") authorization: String,
        @Path("paymentId") paymentId: String,
        @Query("localisedError") localisedError: String?
    ): Single<BankTransferChargeResponse>

    @GET("payments/google-pay/info")
    fun getGooglePayInfo(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String
    ): Single<GooglePayResponse>

    @POST("payments/bind/beneficiary")
    suspend fun getBeneficiaryInfo(
        @Header("authorization") authorization: String,
        @Body body: AliasInfoRequestBody,
        @Query("localisedError") localisedError: String?
    ): Outcome<ApiException, AliasInfoResponse>

    @PUT("payments/bind/beneficiary")
    suspend fun activateBeneficiary(
        @Header("authorization") authorization: String,
        @Body body: LinkWithAliasRequestBody
    ): Outcome<ApiException, Unit>

    @GET("payments/cards/success-rate")
    suspend fun checkNewCardRejectionState(
        @Header("authorization") authorization: String,
        @Query("bin") binNumber: String
    ): Outcome<ApiException, CardRejectionStateResponse>
}
