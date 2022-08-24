package com.blockchain.nabu.api.nabu

import com.blockchain.nabu.models.responses.cards.PaymentCardAcquirerResponse
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.ApplicantIdRequest
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
import com.blockchain.core.sdd.domain.model.SddEligibilityDto
import com.blockchain.core.sdd.domain.model.SddStatusDto
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.BuyOrderListResponse
import com.blockchain.nabu.models.responses.simplebuy.BuySellOrderResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.DepositRequestBody
import com.blockchain.nabu.models.responses.simplebuy.FeesResponse
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyResponse
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
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

    @PUT(NABU_USERS_CURRENT)
    fun createBasicUser(
        @Body basicUser: NabuBasicUser,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Completable

    @GET(NABU_USERS_CURRENT)
    fun getUser(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<NabuUser>

    @GET(NABU_AIRDROP_CENTRE)
    fun getAirdropCampaignStatus(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<AirdropStatusList>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<NabuUser>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Completable

    /**
     * This is a GET, but it actually starts a veriff session on the server for historical reasons.
     * So do not call more than once per veriff launch.
     */

    @GET(NABU_VERIFF_TOKEN)
    fun startVeriffSession(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
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
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Completable

    @PUT(NABU_FETCH_EXCHANGE_ADDRESS_FOR_WALLET)
    fun fetchExchangeSendAddress(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body currency: SendToExchangeAddressRequest
    ): Single<SendToExchangeAddressResponse>

    @AuthenticationNotRequired
    @GET(SDD_ELIGIBLE)
    fun isSDDEligible(): Single<SddEligibilityDto>

    @GET(SDD_VERIFIED)
    fun isSDDVerified(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<SddStatusDto>

    @AuthenticationNotRequired
    @GET(NABU_SIMPLE_BUY_PAIRS)
    fun getSupportedSimpleBuyPairs(
        @Query("fiatCurrency") fiatCurrency: String? = null
    ): Single<SimpleBuyPairsDto>

    @GET(NABU_SIMPLE_BUY_TRANSACTIONS)
    fun getTransactions(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("product") product: String,
        @Query("limit") limit: Int = 100,
        @Query("currency") currency: String? = null,
        @Query("type") type: String?
    ): Single<TransactionsResponse>

    @PUT(NABU_SIMPLE_BUY_ACCOUNT_DETAILS)
    fun getSimpleBuyBankAccountDetails(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body currency: SimpleBuyCurrency
    ): Single<BankAccountResponse>

    @GET(NABU_SIMPLE_BUY_ELIGIBILITY)
    fun isEligibleForSimpleBuy(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("fiatCurrency") fiatCurrency: String?,
        @Query("methods") methods: String = "BANK_ACCOUNT,PAYMENT_CARD"
    ): Single<SimpleBuyEligibilityDto>

    @POST(NABU_SIMPLE_BUY_ORDERS)
    fun createOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("action") action: String?,
        @Body order: CustodialWalletOrder,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_TRADES_WITHDRAW_FEES_AND_LIMITS)
    fun getWithdrawFeeAndLimits(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("product") product: String,
        @Query("paymentMethod") type: String
    ): Single<FeesResponse>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_WITHDRAW_ORDER)
    fun withdrawOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body withdrawRequestBody: WithdrawRequestBody
    ): Completable

    @POST(NABU_DEPOSIT_ORDER)
    fun createDepositOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body depositRequestBody: DepositRequestBody
    ): Completable

    @POST("$NABU_UPDATE_ORDER/{id}")
    fun updateOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("id") id: String,
        @Body body: UpdateSwapOrderBody
    ): Completable

    @GET(NABU_SWAP_ORDER)
    fun getSwapOrders(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<List<CustodialOrderResponse>>

    @GET(NABU_SWAP_PAIRS)
    fun getSwapAvailablePairs(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<List<String>>

    @GET(NABU_SIMPLE_BUY_ORDERS)
    fun getOrders(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("pendingOnly") pendingOnly: Boolean,
        @Query("localisedError") localisedError: String?
    ): Single<BuyOrderListResponse>

    @POST(NABU_WITHDRAW_LOCKS_CHECK)
    fun getWithdrawalLocksCheck(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body withdrawLocksCheckRequestBody: WithdrawLocksCheckRequestBody
    ): Single<WithdrawLocksCheckResponse>

    @DELETE("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun deleteBuyOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Completable

    @GET("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun getBuyOrder(
        @Header("authorization") authHeader: String, // FLAG_AUTH_REMOVAL
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @POST("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun confirmOrder(
        @Header("authorization") authHeader: String, // FLAG_AUTH_REMOVAL
        @Path("orderId") orderId: String,
        @Body confirmBody: ConfirmOrderRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_ELIGIBLE_PAYMENT_METHODS)
    fun getPaymentMethodsForSimpleBuy(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("currency") currency: String,
        @Query("tier") tier: Int?,
        @Query("eligibleOnly") eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>>

    @GET(NABU_CARD_ACQUIRERS)
    fun getCardAcquirers(
        @Header("authorization") authorization: String // FLAG_AUTH_REMOVAL
    ): Single<List<PaymentCardAcquirerResponse>>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_BALANCE_TRANSFER)
    fun transferFunds(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body request: TransferRequest
    ): Single<TransferFundsResponse>

    @POST(NABU_QUOTES)
    fun fetchQuote(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body quoteRequest: QuoteRequest
    ): Single<QuoteResponse>

    @POST(NABU_SWAP_ORDER)
    fun createCustodialOrder(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body order: CreateOrderRequest,
        @Query("localisedError") localisedError: String?
    ): Single<CustodialOrderResponse>

    @GET(NABU_LIMITS)
    fun fetchLimits(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("currency") currency: String,
        @Query("product") product: String,
        @Query("minor") useMinor: Boolean = true,
        @Query("side") side: String?,
        @Query("orderDirection") orderDirection: String?
    ): Single<SwapLimitsResponse>

    @GET(NABU_SWAP_ACTIVITY)
    fun fetchSwapActivity(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("limit") limit: Int = 50
    ): Single<List<CustodialOrderResponse>>

    @POST(NABU_TRANSFER)
    fun executeTransfer(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body body: ProductTransferRequestBody
    ): Completable

    @POST(NABU_RECURRING_BUY_CREATE)
    fun createRecurringBuy(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Body recurringBuyBody: RecurringBuyRequestBody
    ): Single<RecurringBuyResponse>

    @GET(NABU_RECURRING_BUY_LIST)
    fun getRecurringBuyById(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Query("id") recurringBuyId: String
    ): Single<List<RecurringBuyResponse>>

    @DELETE("$NABU_RECURRING_BUY/{id}/cancel")
    fun cancelRecurringBuy(
        @Header("authorization") authorization: String, // FLAG_AUTH_REMOVAL
        @Path("id") id: String
    ): Completable
}
