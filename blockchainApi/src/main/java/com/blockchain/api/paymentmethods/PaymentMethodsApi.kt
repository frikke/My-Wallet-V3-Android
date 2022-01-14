package com.blockchain.api.paymentmethods

import com.blockchain.api.paymentmethods.models.ActivateCardResponse
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.AddNewCardResponse
import com.blockchain.api.paymentmethods.models.BankInfoResponse
import com.blockchain.api.paymentmethods.models.BankTransferChargeResponse
import com.blockchain.api.paymentmethods.models.BankTransferPaymentBody
import com.blockchain.api.paymentmethods.models.BankTransferPaymentResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.paymentmethods.models.CreateLinkBankRequestBody
import com.blockchain.api.paymentmethods.models.CreateLinkBankResponse
import com.blockchain.api.paymentmethods.models.LinkedBankTransferResponse
import com.blockchain.api.paymentmethods.models.OpenBankingTokenBody
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.api.paymentmethods.models.UpdateProviderAccountBody
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
        @Body addNewCardBody: AddNewCardBodyRequest
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
        @Path("id") id: String
    ): Single<LinkedBankTransferResponse>

    @DELETE("payments/banks/{id}")
    fun removeBeneficiary(
        @Header("authorization") authHeader: String,
        @Path("id") id: String
    ): Completable

    @DELETE("payments/banktransfer/{id}")
    fun removeLinkedBank(
        @Header("authorization") authHeader: String,
        @Path("id") id: String
    ): Completable

    @POST("payments/banktransfer")
    fun linkBank(
        @Header("authorization") authorization: String,
        @Body body: CreateLinkBankRequestBody
    ): Single<CreateLinkBankResponse>

    @POST("payments/banktransfer/{id}/update")
    fun updateProviderAccount(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: UpdateProviderAccountBody
    ): Completable

    @POST("payments/banktransfer/{id}/payment")
    fun startBankTransferPayment(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: BankTransferPaymentBody
    ): Single<BankTransferPaymentResponse>

    @POST
    fun updateOpenBankingToken(
        @Url url: String,
        @Header("authorization") authorization: String,
        @Body body: OpenBankingTokenBody
    ): Completable

    @GET("payments/payment/{paymentId}")
    fun getBankTransferCharge(
        @Header("authorization") authorization: String,
        @Path("paymentId") paymentId: String
    ): Single<BankTransferChargeResponse>
}
