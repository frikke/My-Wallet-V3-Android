package com.blockchain.nabu.api.nabu

import com.blockchain.nabu.models.responses.cards.PaymentCardAcquirerResponse
import com.blockchain.nabu.models.responses.cards.PaymentMethodResponse
import com.blockchain.nabu.models.responses.interest.InterestAddressResponse
import com.blockchain.nabu.models.responses.interest.InterestEnabledResponse
import com.blockchain.nabu.models.responses.interest.InterestLimitsFullResponse
import com.blockchain.nabu.models.responses.interest.InterestRateResponse
import com.blockchain.nabu.models.responses.interest.InterestWithdrawalBody
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
import com.blockchain.nabu.models.responses.nabu.TierUpdateJson
import com.blockchain.nabu.models.responses.nabu.TiersResponse
import com.blockchain.nabu.models.responses.nabu.VeriffToken
import com.blockchain.nabu.models.responses.sdd.SDDEligibilityResponse
import com.blockchain.nabu.models.responses.sdd.SDDStatusResponse
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
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibility
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsResp
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
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
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

    @POST(NABU_INITIAL_AUTH)
    fun getAuthToken(
        @Body jwt: NabuOfflineTokenRequest,
        @Query("fiatCurrency") currency: String? = null,
        @Query("action") action: String? = null
    ): Single<NabuOfflineTokenResponse>

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
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_USERS_CURRENT)
    fun getUser(
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_AIRDROP_CENTRE)
    fun getAirdropCampaignStatus(
        @Header("authorization") authorization: String
    ): Single<AirdropStatusList>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
        @Header("authorization") authorization: String
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
        @Header("authorization") authorization: String
    ): Completable

    /**
     * This is a GET, but it actually starts a veriff session on the server for historical reasons.
     * So do not call more than once per veriff launch.
     */

    @GET(NABU_VERIFF_TOKEN)
    fun startVeriffSession(
        @Header("authorization") authorization: String
    ): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST("$NABU_RECOVER_ACCOUNT/{userId}")
    fun recoverAccount(
        @Path("userId") userId: String,
        @Body recoverAccountRequest: NabuRecoverAccountRequest
    ): Single<NabuRecoverAccountResponse>

    @POST("$NABU_RECOVER_USER/{userId}")
    fun recoverUser(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

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
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_KYC_TIERS)
    fun getTiers(
        @Header("authorization") authorization: String
    ): Single<TiersResponse>

    @POST(NABU_KYC_TIERS)
    fun setTier(
        @Body tierUpdateJson: TierUpdateJson,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_FETCH_EXCHANGE_ADDRESS_FOR_WALLET)
    fun fetchExchangeSendAddress(
        @Header("authorization") authorization: String,
        @Body currency: SendToExchangeAddressRequest
    ): Single<SendToExchangeAddressResponse>

    @GET(SDD_ELIGIBLE)
    fun isSDDEligible(): Single<SDDEligibilityResponse>

    @GET(SDD_VERIFIED)
    fun isSDDVerified(@Header("authorization") authorization: String): Single<SDDStatusResponse>

    @GET(NABU_SIMPLE_BUY_PAIRS)
    fun getSupportedSimpleBuyPairs(
        @Query("fiatCurrency") fiatCurrency: String? = null
    ): Single<SimpleBuyPairsResp>

    @GET(NABU_SIMPLE_BUY_TRANSACTIONS)
    fun getTransactions(
        @Header("authorization") authorization: String,
        @Query("product") product: String,
        @Query("limit") limit: Int = 100,
        @Query("currency") currency: String? = null,
        @Query("type") type: String?
    ): Single<TransactionsResponse>

    @PUT(NABU_SIMPLE_BUY_ACCOUNT_DETAILS)
    fun getSimpleBuyBankAccountDetails(
        @Header("authorization") authorization: String,
        @Body currency: SimpleBuyCurrency
    ): Single<BankAccountResponse>

    @GET(NABU_SIMPLE_BUY_ELIGIBILITY)
    fun isEligibleForSimpleBuy(
        @Header("authorization") authorization: String,
        @Query("fiatCurrency") fiatCurrency: String?,
        @Query("methods") methods: String = "BANK_ACCOUNT,PAYMENT_CARD"
    ): Single<SimpleBuyEligibility>

    @POST(NABU_SIMPLE_BUY_ORDERS)
    fun createOrder(
        @Header("authorization") authorization: String,
        @Query("action") action: String?,
        @Body order: CustodialWalletOrder,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_TRADES_WITHDRAW_FEES_AND_LIMITS)
    fun getWithdrawFeeAndLimits(
        @Header("authorization") authorization: String,
        @Query("product") product: String,
        @Query("paymentMethod") type: String
    ): Single<FeesResponse>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_WITHDRAW_ORDER)
    fun withdrawOrder(
        @Header("authorization") authorization: String,
        @Body withdrawRequestBody: WithdrawRequestBody
    ): Completable

    @POST(NABU_DEPOSIT_ORDER)
    fun createDepositOrder(
        @Header("authorization") authorization: String,
        @Body depositRequestBody: DepositRequestBody
    ): Completable

    @POST("$NABU_UPDATE_ORDER/{id}")
    fun updateOrder(
        @Header("authorization") authorization: String,
        @Path("id") id: String,
        @Body body: UpdateSwapOrderBody
    ): Completable

    @GET(NABU_SWAP_ORDER)
    fun getSwapOrders(@Header("authorization") authorization: String): Single<List<CustodialOrderResponse>>

    @GET(NABU_SWAP_PAIRS)
    fun getSwapAvailablePairs(@Header("authorization") authorization: String): Single<List<String>>

    @GET(NABU_SIMPLE_BUY_ORDERS)
    fun getOrders(
        @Header("authorization") authorization: String,
        @Query("pendingOnly") pendingOnly: Boolean,
        @Query("localisedError") localisedError: String?
    ): Single<BuyOrderListResponse>

    @POST(NABU_WITHDRAW_LOCKS_CHECK)
    fun getWithdrawalLocksCheck(
        @Header("authorization") authorization: String,
        @Body withdrawLocksCheckRequestBody: WithdrawLocksCheckRequestBody
    ): Single<WithdrawLocksCheckResponse>

    @DELETE("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun deleteBuyOrder(
        @Header("authorization") authorization: String,
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Completable

    @GET("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun getBuyOrder(
        @Header("authorization") authHeader: String,
        @Path("orderId") orderId: String,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @POST("$NABU_SIMPLE_BUY_ORDERS/{orderId}")
    fun confirmOrder(
        @Header("authorization") authHeader: String,
        @Path("orderId") orderId: String,
        @Body confirmBody: ConfirmOrderRequestBody,
        @Query("localisedError") localisedError: String?
    ): Single<BuySellOrderResponse>

    @GET(NABU_ELIGIBLE_PAYMENT_METHODS)
    fun getPaymentMethodsForSimpleBuy(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String,
        @Query("tier") tier: Int?,
        @Query("eligibleOnly") eligibleOnly: Boolean
    ): Single<List<PaymentMethodResponse>>

    @GET(NABU_CARD_ACQUIRERS)
    fun getCardAcquirers(
        @Header("authorization") authorization: String
    ): Single<List<PaymentCardAcquirerResponse>>

    @Headers("blockchain-origin: simplebuy")
    @POST(NABU_SIMPLE_BUY_BALANCE_TRANSFER)
    fun transferFunds(
        @Header("authorization") authorization: String,
        @Body request: TransferRequest
    ): Single<TransferFundsResponse>

    @GET(NABU_INTEREST_RATES)
    fun getInterestRates(
        @Header("authorization") authorization: String,
        @Query("ccy") currency: String
    ): Single<Response<InterestRateResponse>>

    @GET(NABU_INTEREST_ADDRESS)
    fun getInterestAddress(
        @Header("authorization") authorization: String,
        @Query("ccy") currency: String
    ): Single<InterestAddressResponse>

    @GET(NABU_INTEREST_LIMITS)
    fun getInterestLimits(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String
    ): Single<InterestLimitsFullResponse>

    @POST(NABU_INTEREST_WITHDRAWAL)
    fun createInterestWithdrawal(
        @Header("authorization") authorization: String,
        @Body body: InterestWithdrawalBody
    ): Completable

    @GET(NABU_INTEREST_AVAILABLE_TICKERS)
    fun getAvailableTickersForInterest(
        @Header("authorization") authorization: String
    ): Single<InterestEnabledResponse>

    @POST(NABU_QUOTES)
    fun fetchQuote(
        @Header("authorization") authorization: String,
        @Body quoteRequest: QuoteRequest
    ): Single<QuoteResponse>

    @POST(NABU_SWAP_ORDER)
    fun createCustodialOrder(
        @Header("authorization") authorization: String,
        @Body order: CreateOrderRequest,
        @Query("localisedError") localisedError: String?
    ): Single<CustodialOrderResponse>

    @GET(NABU_LIMITS)
    fun fetchLimits(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String,
        @Query("product") product: String,
        @Query("minor") useMinor: Boolean = true,
        @Query("side") side: String?,
        @Query("orderDirection") orderDirection: String?
    ): Single<SwapLimitsResponse>

    @GET(NABU_SWAP_ACTIVITY)
    fun fetchSwapActivity(
        @Header("authorization") authorization: String,
        @Query("limit") limit: Int = 50
    ): Single<List<CustodialOrderResponse>>

    @POST(NABU_TRANSFER)
    fun executeTransfer(
        @Header("authorization") authorization: String,
        @Body body: ProductTransferRequestBody
    ): Completable

    @POST(NABU_RECURRING_BUY_CREATE)
    fun createRecurringBuy(
        @Header("authorization") authorization: String,
        @Body recurringBuyBody: RecurringBuyRequestBody
    ): Single<RecurringBuyResponse>

    @GET(NABU_RECURRING_BUY_LIST)
    fun getRecurringBuyById(
        @Header("authorization") authorization: String,
        @Query("id") recurringBuyId: String
    ): Single<List<RecurringBuyResponse>>

    @DELETE("$NABU_RECURRING_BUY/{id}/cancel")
    fun cancelRecurringBuy(
        @Header("authorization") authorization: String,
        @Path("id") id: String
    ): Completable
}
