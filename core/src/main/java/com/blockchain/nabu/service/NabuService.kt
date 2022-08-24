package com.blockchain.nabu.service

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.nabu.datamanagers.TransactionError
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
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.core.sdd.domain.model.SddEligibilityDto
import com.blockchain.core.sdd.domain.model.SddStatusDto
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.DepositRequestBody
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyCurrency
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsResp
import com.blockchain.nabu.models.responses.simplebuy.TransactionsResponse
import com.blockchain.nabu.models.responses.simplebuy.TransferRequest
import com.blockchain.nabu.models.responses.simplebuy.WithdrawLocksCheckRequestBody
import com.blockchain.nabu.models.responses.simplebuy.WithdrawRequestBody
import com.blockchain.nabu.models.responses.swap.CreateOrderRequest
import com.blockchain.nabu.models.responses.swap.CustodialOrderResponse
import com.blockchain.nabu.models.responses.swap.QuoteRequest
import com.blockchain.nabu.models.responses.swap.QuoteResponse
import com.blockchain.nabu.models.responses.swap.SwapLimitsResponse
import com.blockchain.nabu.models.responses.swap.UpdateSwapOrderBody
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineToken
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.preferences.RemoteConfigPrefs
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class NabuService internal constructor(
    private val nabu: Nabu,
    private val remoteConfigPrefs: RemoteConfigPrefs,
    private val environmentConfig: EnvironmentConfig
) {
    internal fun getAuthToken(
        jwt: String
    ): Single<NabuOfflineTokenResponse> = nabu.getAuthToken(
        jwt = NabuOfflineTokenRequest(jwt)
    ).wrapErrorMessage()

    internal fun getSessionToken(
        userId: String,
        offlineToken: String,
        guid: String,
        email: String,
        appVersion: String,
        deviceId: String
    ): Single<NabuSessionTokenResponse> = nabu.getSessionToken(
        userId,
        offlineToken,
        guid,
        email,
        appVersion,
        CLIENT_TYPE,
        deviceId
    ).wrapErrorMessage()

    internal fun createBasicUser(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        sessionToken: NabuSessionTokenResponse
    ): Completable = nabu.createBasicUser(
        NabuBasicUser(firstName, lastName, dateOfBirth),
        sessionToken.authHeader
    )

    internal fun getUser(
        sessionToken: NabuSessionTokenResponse
    ): Single<NabuUser> = nabu.getUser(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getAirdropCampaignStatus(
        sessionToken: NabuSessionTokenResponse
    ): Single<AirdropStatusList> = nabu.getAirdropCampaignStatus(
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun updateWalletInformation(
        sessionToken: NabuSessionTokenResponse,
        jwt: String
    ): Single<NabuUser> = nabu.updateWalletInformation(
        NabuJwt(jwt),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun getSupportedDocuments(
        sessionToken: NabuSessionTokenResponse,
        countryCode: String
    ): Single<List<SupportedDocuments>> = nabu.getSupportedDocuments(
        countryCode,
        sessionToken.authHeader
    ).wrapErrorMessage()
        .map { it.documentTypes }

    internal fun addAddress(
        sessionToken: NabuSessionTokenResponse,
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryCode: String
    ): Completable = nabu.addAddress(
        AddAddressRequest.fromAddressDetails(
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recordCountrySelection(
        sessionToken: NabuSessionTokenResponse,
        jwt: String,
        countryCode: String,
        stateCode: String?,
        notifyWhenAvailable: Boolean
    ): Completable = nabu.recordSelectedCountry(
        RecordCountryRequest(
            jwt,
            countryCode,
            notifyWhenAvailable,
            stateCode
        ),
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun startVeriffSession(
        sessionToken: NabuSessionTokenResponse
    ): Single<VeriffApplicantAndToken> = nabu.startVeriffSession(
        sessionToken.authHeader
    ).map { VeriffApplicantAndToken(it.applicantId, it.token) }
        .wrapErrorMessage()

    internal fun submitVeriffVerification(
        sessionToken: NabuSessionTokenResponse
    ): Completable = nabu.submitVerification(
        ApplicantIdRequest(sessionToken.userId), // FLAG_AUTH_REMOVAL
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun recoverAccount(
        userId: String,
        jwt: String,
        recoveryToken: String
    ): Single<NabuRecoverAccountResponse> = nabu.recoverAccount(
        userId,
        NabuRecoverAccountRequest(
            jwt = jwt,
            recoveryToken = recoveryToken
        )
    ).wrapErrorMessage()

    internal fun recoverUser(
        offlineToken: NabuOfflineToken,
        jwt: String
    ): Completable = nabu.recoverUser(
        offlineToken.userId, // FLAG_AUTH_REMOVAL
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun resetUserKyc(
        offlineToken: NabuOfflineToken,
        jwt: String
    ): Completable = nabu.resetUserKyc(
        offlineToken.userId, // FLAG_AUTH_REMOVAL
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun registerCampaign(
        sessionToken: NabuSessionTokenResponse,
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = nabu.registerCampaign(
        campaignRequest,
        campaignName,
        sessionToken.authHeader
    ).wrapErrorMessage()

    internal fun fetchExchangeSendToAddressForCrypto(
        sessionToken: NabuSessionTokenResponse,
        cryptoSymbol: String
    ): Single<SendToExchangeAddressResponse> = nabu.fetchExchangeSendAddress(
        sessionToken.authHeader,
        SendToExchangeAddressRequest(cryptoSymbol)
    ).wrapErrorMessage()

    internal fun isSDDEligible(): Single<SddEligibilityDto> =
        nabu.isSDDEligible().wrapErrorMessage()

    internal fun isSDDVerified(sessionToken: NabuSessionTokenResponse): Single<SddStatusDto> =
        nabu.isSDDVerified(
            sessionToken.authHeader
        ).wrapErrorMessage()

    internal fun fetchQuote(
        sessionToken: NabuSessionTokenResponse,
        quoteRequest: QuoteRequest
    ): Single<QuoteResponse> = nabu.fetchQuote(
        sessionToken.authHeader,
        quoteRequest
    ).wrapErrorMessage()

    internal fun createCustodialOrder(
        sessionToken: NabuSessionTokenResponse,
        createOrderRequest: CreateOrderRequest
    ): Single<CustodialOrderResponse> = nabu.createCustodialOrder(
        authorization = sessionToken.authHeader,
        order = createOrderRequest,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    internal fun fetchProductLimits(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        product: String,
        side: String?,
        orderDirection: String?
    ): Single<SwapLimitsResponse> = nabu.fetchLimits(
        authorization = sessionToken.authHeader,
        currency = currency,
        product = product,
        side = side,
        orderDirection = orderDirection
    ).onErrorResumeNext {
        if ((it as? HttpException)?.code() == 409) {
            Single.just(
                SwapLimitsResponse()
            )
        } else {
            Single.error(it)
        }
    }.wrapErrorMessage()

    internal fun fetchSwapActivity(
        sessionToken: NabuSessionTokenResponse
    ): Single<List<CustodialOrderResponse>> =
        nabu.fetchSwapActivity(sessionToken.authHeader).wrapErrorMessage()

    internal fun getSupportedCurrencies(
        fiatCurrency: String? = null
    ): Single<SimpleBuyPairsResp> =
        nabu.getSupportedSimpleBuyPairs(fiatCurrency).wrapErrorMessage()

    fun getSimpleBuyBankAccountDetails(
        sessionToken: NabuSessionTokenResponse,
        currency: String
    ): Single<BankAccountResponse> =
        nabu.getSimpleBuyBankAccountDetails(
            sessionToken.authHeader, SimpleBuyCurrency(currency)
        ).wrapErrorMessage()

    internal fun getTransactions(
        sessionToken: NabuSessionTokenResponse,
        product: String,
        type: String?
    ): Single<TransactionsResponse> =
        nabu.getTransactions(
            authorization = sessionToken.authHeader,
            product = product,
            type = type
        ).wrapErrorMessage()

    internal fun getCurrencyTransactions(
        sessionToken: NabuSessionTokenResponse,
        product: String,
        currency: String,
        type: String?
    ): Single<TransactionsResponse> =
        nabu.getTransactions(
            authorization = sessionToken.authHeader,
            product = product,
            currency = currency,
            type = type
        ).wrapErrorMessage()

    internal fun isEligibleForSimpleBuy(
        sessionToken: NabuSessionTokenResponse,
        fiatCurrency: String? = null
    ): Single<SimpleBuyEligibilityDto> = nabu.isEligibleForSimpleBuy(
        sessionToken.authHeader,
        fiatCurrency
    ).wrapErrorMessage()

    internal fun createOrder(
        sessionToken: NabuSessionTokenResponse,
        order: CustodialWalletOrder,
        action: String?
    ) = nabu.createOrder(
        authorization = sessionToken.authHeader,
        action = action,
        order = order,
        localisedError = getLocalisedErrorIfEnabled()
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Single.error(TransactionError.OrderLimitReached)
        } else {
            Single.error(it)
        }
    }.wrapErrorMessage()

    fun createRecurringBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        recurringOrderBody: RecurringBuyRequestBody
    ) = nabu.createRecurringBuy(
        authorization = sessionToken.authHeader,
        recurringBuyBody = recurringOrderBody
    ).wrapErrorMessage()

    internal fun fetchWithdrawFeesAndLimits(
        sessionToken: NabuSessionTokenResponse,
        product: String,
        paymentMethod: String
    ) = nabu.getWithdrawFeeAndLimits(
        sessionToken.authHeader, product, paymentMethod
    ).wrapErrorMessage()

    internal fun fetchWithdrawLocksRules(
        sessionToken: NabuSessionTokenResponse,
        paymentMethod: PaymentMethodType,
        fiatCurrency: String
    ) = nabu.getWithdrawalLocksCheck(
        sessionToken.authHeader,
        WithdrawLocksCheckRequestBody(
            paymentMethod = paymentMethod.name, currency = fiatCurrency
        )
    ).wrapErrorMessage()

    internal fun createWithdrawOrder(
        sessionToken: NabuSessionTokenResponse,
        amount: String,
        currency: String,
        beneficiaryId: String
    ) = nabu.withdrawOrder(
        sessionToken.authHeader,
        WithdrawRequestBody(beneficiary = beneficiaryId, amount = amount, currency = currency)
    ).wrapErrorMessage()

    internal fun createDepositTransaction(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        address: String,
        hash: String,
        amount: String,
        product: String
    ) = nabu.createDepositOrder(
        sessionToken.authHeader,
        DepositRequestBody(
            currency = currency, depositAddress = address, txHash = hash, amount = amount, product = product
        )
    )

    internal fun updateOrder(
        sessionToken: NabuSessionTokenResponse,
        id: String,
        success: Boolean
    ) = nabu.updateOrder(
        sessionToken.authHeader,
        id,
        UpdateSwapOrderBody.newInstance(success)
    ).wrapErrorMessage()

    internal fun getOutstandingOrders(
        sessionToken: NabuSessionTokenResponse,
        pendingOnly: Boolean
    ) = nabu.getOrders(
        authorization = sessionToken.authHeader,
        pendingOnly = pendingOnly,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    internal fun getSwapTrades(sessionToken: NabuSessionTokenResponse) = nabu.getSwapOrders(sessionToken.authHeader)

    internal fun getSwapAvailablePairs(sessionToken: NabuSessionTokenResponse) =
        nabu.getSwapAvailablePairs(sessionToken.authHeader)

    internal fun deleteBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = nabu.deleteBuyOrder(
        authorization = sessionToken.authHeader,
        orderId = orderId,
        localisedError = getLocalisedErrorIfEnabled()
    ).onErrorResumeNext {
        if (it is HttpException && it.code() == 409) {
            Completable.error(TransactionError.OrderNotCancelable)
        } else {
            Completable.error(it)
        }
    }.wrapErrorMessage()

    fun getBuyOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String
    ) = nabu.getBuyOrder(
        authHeader = sessionToken.authHeader,
        orderId = orderId,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    fun confirmOrder(
        sessionToken: NabuSessionTokenResponse,
        orderId: String,
        confirmBody: ConfirmOrderRequestBody
    ) = nabu.confirmOrder(
        authHeader = sessionToken.authHeader,
        orderId = orderId,
        confirmBody = confirmBody,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    fun transferFunds(
        sessionToken: NabuSessionTokenResponse,
        request: TransferRequest
    ): Single<String> = nabu.transferFunds(
        sessionToken.authHeader,
        request
    ).map {
        it.id
    }.wrapErrorMessage()

    fun paymentMethods(
        sessionToken: NabuSessionTokenResponse,
        currency: String,
        eligibleOnly: Boolean,
        tier: Int? = null
    ) = nabu.getPaymentMethodsForSimpleBuy(
        authorization = sessionToken.authHeader,
        currency = currency,
        tier = tier,
        eligibleOnly = eligibleOnly
    ).wrapErrorMessage()

    fun cardAcquirers(
        sessionToken: NabuSessionTokenResponse
    ) = nabu.getCardAcquirers(sessionToken.authHeader).wrapErrorMessage()

    fun executeTransfer(
        sessionToken: NabuSessionTokenResponse,
        body: ProductTransferRequestBody
    ) = nabu.executeTransfer(
        authorization = sessionToken.authHeader,
        body = body
    ).wrapErrorMessage()

    fun getRecurringBuyForId(
        sessionToken: NabuSessionTokenResponse,
        recurringBuyId: String
    ) = nabu.getRecurringBuyById(
        authorization = sessionToken.authHeader,
        recurringBuyId = recurringBuyId
    ).wrapErrorMessage()

    fun cancelRecurringBuy(
        sessionToken: NabuSessionTokenResponse,
        id: String
    ) = nabu.cancelRecurringBuy(
        authorization = sessionToken.authHeader,
        id = id
    ).wrapErrorMessage()

    private fun getLocalisedErrorIfEnabled(): String? =
        if (environmentConfig.isRunningInDebugMode() && remoteConfigPrefs.brokerageErrorsEnabled) {
            remoteConfigPrefs.brokerageErrorsCode
        } else {
            null
        }

    companion object {
        internal const val CLIENT_TYPE = "APP"
    }
}
