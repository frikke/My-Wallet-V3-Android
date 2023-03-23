package com.blockchain.nabu.api.nabu

import com.blockchain.core.sdd.domain.model.SddEligibilityDto
import com.blockchain.core.sdd.domain.model.SddStatusDto
import com.blockchain.nabu.models.responses.cards.PaymentCardAcquirerResponse
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.ApplicantIdRequest
import com.blockchain.nabu.models.responses.nabu.IsProfileNameValidRequest
import com.blockchain.nabu.models.responses.nabu.NabuBasicUser
import com.blockchain.nabu.models.responses.nabu.NabuJwt
import com.blockchain.nabu.models.responses.nabu.NabuRecoverAccountRequest
import com.blockchain.nabu.models.responses.nabu.NabuRecoverAccountResponse
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.RecordCountryRequest
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.SendToExchangeAddressRequest
import com.blockchain.nabu.models.responses.nabu.SendToExchangeAddressResponse
import com.blockchain.nabu.models.responses.nabu.SupportedDocumentsResponse
import com.blockchain.nabu.models.responses.nabu.VeriffToken
import com.blockchain.nabu.models.responses.simplebuy.BuyOrderListResponse
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.DepositRequestBody
import com.blockchain.nabu.models.responses.simplebuy.FeesResponse
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyCurrency
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsDto
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferFundsResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferRequest
import com.blockchain.nabu.models.responses.simplebuy.WithdrawLocksCheckRequestBody
import com.blockchain.nabu.models.responses.simplebuy.WithdrawLocksCheckResponse
import com.blockchain.nabu.models.responses.simplebuy.WithdrawRequestBody
import com.blockchain.nabu.models.responses.swap.CreateOrderRequest
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.nabu.models.responses.swap.QuoteRequest
import com.blockchain.nabu.models.responses.swap.QuoteResponse
import com.blockchain.nabu.models.responses.swap.SwapLimitsResponse
import com.blockchain.nabu.models.responses.swap.UpdateSwapOrderBody
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.network.interceptor.AuthenticationNotRequired
import com.blockchain.network.interceptor.CustomAuthentication
import com.blockchain.outcome.Outcome
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface Nabu {

    @AuthenticationNotRequired
    @POST(NABU_INITIAL_AUTH)
    fun getAuthToken(
        @Body jwt: NabuOfflineTokenRequest,
        @Query("fiatCurrency") currency: String? = null,
        @Query("action") action: String? = null
    ): Single<NabuOfflineTokenResponse>

    @CustomAuthentication
    @POST(NABU_SESSION_TOKEN)
    fun getSessionToken(
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuSessionTokenResponse>

    @PUT("users/current")
    suspend fun createBasicUser(
        @Body basicUser: NabuBasicUser,
    ): Outcome<Exception, Unit>

    @POST("validation/person-name")
    suspend fun isProfileNameValid(
        @Body request: IsProfileNameValidRequest,
    ): Outcome<Exception, Unit>

    @GET("users/current")
    fun getUser(): Single<NabuUser>

    @PATCH(NABU_USERS_TAGS_SYNC)
    fun syncUserTags(@Body flags: UserTags): Completable

    @GET(NABU_AIRDROP_CENTRE)
    fun getAirdropCampaignStatus(): Single<AirdropStatusList>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
    ): Single<NabuUser>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
    ): Completable

    /**
     * This is a GET, but it actually starts a veriff session on the server for historical reasons.
     * So do not call more than once per veriff launch.
     */

    @GET(NABU_VERIFF_TOKEN)
    fun startVeriffSession(): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
    ): Completable

    @AuthenticationNotRequired
    @POST("$NABU_RECOVER_ACCOUNT/{userId}")
    fun recoverAccount(
        @Path("userId") userId: String,
        @Body recoverAccountRequest: NabuRecoverAccountRequest
    ): Single<NabuRecoverAccountResponse>

    @CustomAuthentication
    @POST("$NABU_RECOVER_USER/{userId}")
    fun recoverUser(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

    @CustomAuthentication
    @POST("$NABU_RESET_USER/{userId}")
    fun resetUserKyc(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_REGISTER_CAMPAIGN)
    fun registerCampaign(
        @Body campaignRequest: RegisterCampaignRequest,
        @Header("X-CAMPAIGN") campaignHeader: String,
    ): Completable

    @PUT(NABU_FETCH_EXCHANGE_ADDRESS_FOR_WALLET)
    fun fetchExchangeSendAddress(
        @Body currency: SendToExchangeAddressRequest
    ): Single<SendToExchangeAddressResponse>

    @AuthenticationNotRequired
    @GET(SDD_ELIGIBLE)
    fun isSDDEligible(): Single<SddEligibilityDto>

    @GET(SDD_VERIFIED)
    fun isSDDVerified(): Single<SddStatusDto>

    @AuthenticationNotRequired
    @GET(NABU_SIMPLE_BUY_PAIRS)
    fun getSupportedSimpleBuyPairs(
        @Query("fiatCurrency") fiatCurrency: String? = null
    ): Single<SimpleBuyPairsDto>

    @GET(NABU_SIMPLE_BUY_TRANSACTIONS)
    fun getTransactions(
        @Query("product") product: String,
        @Query("limit") limit: Int = 100,
        @Query("currency") currency: String? = null,
        @Query("type") type: String?
    ): Single<TransactionsResponse>

    @PUT("payments/accounts/{product}")
    fun getCustodialAccountDetails(
        @Path("product") product: String,
        @Body currency: SimpleBuyCurrency
    ): Single<CustodialAccountResponse>

    @GET(NABU_SIMPLE_BUY_ELIGIBILITY)
    fun isEligibleForSimpleBuy(
        @Query("fiatCurrency") fiatCurrency: String?,
        @Query("methods") methods: String = "BANK_ACCOUNT,PAYMENT_CARD"
    ): Single<SimpleBuyEligibilityDto>

    @POST(NABU_SIMPLE_BUY_ORDERS)
    fun createOrder(
        @Query("action") action: String?,
        @Body order: CustodialWalletOrder,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_TRADES_WITHDRAW_FEES_AND_LIMITS)
    fun getWithdrawFeeAndLimits(
        @Query("product") product: String,
        @Query("paymentMethod") type: String
    ): Single<FeesResponse>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_WITHDRAW_ORDER)
    fun withdrawOrder(
        @Body withdrawRequestBody: WithdrawRequestBody
    ): Completable

    @POST(NABU_DEPOSIT_ORDER)
    fun createDepositOrder(
        @Body depositRequestBody: DepositRequestBody
    ): Completable

    @POST("custodial/trades/{id}")
    fun updateOrder(
        @Path("id") id: String,
        @Body body: UpdateSwapOrderBody
    ): Completable

    @GET("custodial/trades")
    fun getSwapOrders(): Single<List<CustodialOrderResponse>>

    @GET(NABU_SWAP_PAIRS)
    fun getSwapAvailablePairs(): Single<List<String>>

    @GET(NABU_SIMPLE_BUY_ORDERS)
    fun getOrders(
        @Query("pendingOnly") pendingOnly: Boolean,
        @Query("localisedError") localisedError: String?
    ): Single<BuyOrderListResponse>

    @POST(NABU_WITHDRAW_LOCKS_CHECK)
    fun getWithdrawalLocksCheck(
        @Body withdrawLocksCheckRequestBody: WithdrawLocksCheckRequestBody
    ): Single<WithdrawLocksCheckResponse>

    @DELETE("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun deleteBuyOrder(
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Completable

    @GET("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun getBuyOrder(
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @POST("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun confirmOrder(
        @Path("orderId") orderId: String,
        @Body confirmBody: ConfirmOrderRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_ELIGIBLE_PAYMENT_METHODS)
    fun getPaymentMethodsForSimpleBuy(
        @Query("currency") currency: String,
        @Query("tier") tier: Int?,
        @Query("eligibleOnly") eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>>

    @GET(NABU_CARD_ACQUIRERS)
    fun getCardAcquirers(): Single<List<PaymentCardAcquirerResponse>>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_BALANCE_TRANSFER)
    fun transferFunds(
        @Body request: TransferRequest
    ): Single<TransferFundsResponse>

    @POST(NABU_QUOTES)
    fun fetchQuote(
        @Body quoteRequest: QuoteRequest
    ): Single<QuoteResponse>

    @POST("custodial/trades")
    fun createCustodialOrder(
        @Body order: CreateOrderRequest,
        @Query("localisedError") localisedError: String?
    ): Single<CustodialOrderResponse>

    @GET("trades/limits")
    fun fetchLimits(
        @Query("currency") currency: String,
        @Query("product") product: String,
        @Query("minor") useMinor: Boolean = true,
        @Query("side") side: String?,
        @Query("orderDirection") orderDirection: String?
    ): Single<SwapLimitsResponse>

    @GET(NABU_SWAP_ACTIVITY)
    fun fetchSwapActivity(
        @Query("limit") limit: Int = 50
    ): Single<List<CustodialOrderResponse>>

    @POST(NABU_TRANSFER)
    fun executeTransfer(
        @Body body: ProductTransferRequestBody
    ): Completable
}

@Serializable
data class UserTags(val flags: Map<String, JsonElement>)
