package com.blockchain.nabu.service

import com.blockchain.core.sdd.domain.model.SddEligibilityDto
import com.blockchain.core.sdd.domain.model.SddStatusDto
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.tags.TagsService
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.api.nabu.UserTags
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
import com.blockchain.nabu.models.responses.simplebuy.BankAccountResponse
import com.blockchain.nabu.models.responses.simplebuy.ConfirmOrderRequestBody
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.DepositRequestBody
import com.blockchain.nabu.models.responses.simplebuy.ProductTransferRequestBody
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyCurrency
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyEligibilityDto
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyPairsDto
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
import com.blockchain.utils.thenSingle
import com.blockchain.utils.toJsonElement
import com.blockchain.veriff.VeriffApplicantAndToken
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.HttpException

class NabuService internal constructor(
    private val nabu: Nabu,
    private val remoteConfigPrefs: RemoteConfigPrefs,
    private val tagsService: TagsService,
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
    ): Completable = nabu.createBasicUser(
        NabuBasicUser(firstName, lastName, dateOfBirth),
    )

    internal fun getUser(): Single<NabuUser> = nabu.getUser().flatMap { user ->
        val newTags = tagsService.tags(user.tagKeys)
        if (newTags.isEmpty()) {
            Single.just(user)
        } else {
            nabu.syncUserTags(
                flags = UserTags(
                    newTags.mapValues {
                        it.value.toJsonElement()
                    }
                )
            ).onErrorComplete().thenSingle {
                Single.just(user)
            }
        }
    }.wrapErrorMessage()

    internal fun getAirdropCampaignStatus(): Single<AirdropStatusList> =
        nabu.getAirdropCampaignStatus().wrapErrorMessage()

    internal fun updateWalletInformation(
        jwt: String
    ): Single<NabuUser> = nabu.updateWalletInformation(
        NabuJwt(jwt),
    ).wrapErrorMessage()

    internal fun getSupportedDocuments(
        countryCode: String
    ): Single<List<SupportedDocuments>> = nabu.getSupportedDocuments(
        countryCode,
    ).wrapErrorMessage()
        .map { it.documentTypes }

    internal fun addAddress(
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
    ).wrapErrorMessage()

    internal fun recordCountrySelection(
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
    ).wrapErrorMessage()

    internal fun startVeriffSession(): Single<VeriffApplicantAndToken> =
        nabu.startVeriffSession().map { VeriffApplicantAndToken(it.applicantId, it.token) }
            .wrapErrorMessage()

    internal fun submitVeriffVerification(
        userId: String
    ): Completable = nabu.submitVerification(
        ApplicantIdRequest(userId),
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
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun resetUserKyc(
        offlineToken: NabuOfflineToken,
        jwt: String
    ): Completable = nabu.resetUserKyc(
        offlineToken.userId,
        NabuJwt(jwt),
        authorization = "Bearer ${offlineToken.token}"
    ).wrapErrorMessage()

    internal fun registerCampaign(
        campaignRequest: RegisterCampaignRequest,
        campaignName: String
    ): Completable = nabu.registerCampaign(
        campaignRequest,
        campaignName,
    ).wrapErrorMessage()

    internal fun fetchExchangeSendToAddressForCrypto(
        cryptoSymbol: String
    ): Single<SendToExchangeAddressResponse> = nabu.fetchExchangeSendAddress(
        SendToExchangeAddressRequest(cryptoSymbol)
    ).wrapErrorMessage()

    internal fun isSDDEligible(): Single<SddEligibilityDto> =
        nabu.isSDDEligible().wrapErrorMessage()

    internal fun isSDDVerified(): Single<SddStatusDto> =
        nabu.isSDDVerified().wrapErrorMessage()

    internal fun fetchQuote(
        quoteRequest: QuoteRequest
    ): Single<QuoteResponse> = nabu.fetchQuote(
        quoteRequest
    ).wrapErrorMessage()

    internal fun createCustodialOrder(
        createOrderRequest: CreateOrderRequest
    ): Single<CustodialOrderResponse> = nabu.createCustodialOrder(
        order = createOrderRequest,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    internal fun fetchProductLimits(
        currency: String,
        product: String,
        side: String?,
        orderDirection: String?
    ): Single<SwapLimitsResponse> = nabu.fetchLimits(
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

    internal fun fetchSwapActivity(): Single<List<CustodialOrderResponse>> =
        nabu.fetchSwapActivity().wrapErrorMessage()

    internal fun getSupportedCurrencies(
        fiatCurrency: String? = null
    ): Single<SimpleBuyPairsDto> =
        nabu.getSupportedSimpleBuyPairs(fiatCurrency).wrapErrorMessage()

    fun getSimpleBuyBankAccountDetails(
        currency: String
    ): Single<BankAccountResponse> =
        nabu.getSimpleBuyBankAccountDetails(
            SimpleBuyCurrency(currency)
        ).wrapErrorMessage()

    internal fun getTransactions(
        product: String,
        type: String?
    ): Single<TransactionsResponse> =
        nabu.getTransactions(
            product = product,
            type = type
        ).wrapErrorMessage()

    internal fun getCurrencyTransactions(
        product: String,
        currency: String,
        type: String?
    ): Single<TransactionsResponse> =
        nabu.getTransactions(
            product = product,
            currency = currency,
            type = type
        ).wrapErrorMessage()

    internal fun isEligibleForSimpleBuy(
        fiatCurrency: String? = null
    ): Single<SimpleBuyEligibilityDto> = nabu.isEligibleForSimpleBuy(
        fiatCurrency
    ).wrapErrorMessage()

    internal fun createOrder(
        order: CustodialWalletOrder,
        action: String?
    ) = nabu.createOrder(
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
        recurringOrderBody: RecurringBuyRequestBody
    ) = nabu.createRecurringBuy(
        recurringBuyBody = recurringOrderBody
    ).wrapErrorMessage()

    internal fun fetchWithdrawFeesAndLimits(
        product: String,
        paymentMethod: String
    ) = nabu.getWithdrawFeeAndLimits(
        product, paymentMethod
    ).wrapErrorMessage()

    internal fun fetchWithdrawLocksRules(
        paymentMethod: PaymentMethodType,
        fiatCurrency: String
    ) = nabu.getWithdrawalLocksCheck(
        WithdrawLocksCheckRequestBody(
            paymentMethod = paymentMethod.name, currency = fiatCurrency
        )
    ).wrapErrorMessage()

    internal fun createWithdrawOrder(
        amount: String,
        currency: String,
        beneficiaryId: String
    ) = nabu.withdrawOrder(
        WithdrawRequestBody(beneficiary = beneficiaryId, amount = amount, currency = currency)
    ).wrapErrorMessage()

    internal fun createDepositTransaction(
        currency: String,
        address: String,
        hash: String,
        amount: String,
        product: String
    ) = nabu.createDepositOrder(
        DepositRequestBody(
            currency = currency, depositAddress = address, txHash = hash, amount = amount, product = product
        )
    )

    internal fun updateOrder(
        id: String,
        success: Boolean
    ) = nabu.updateOrder(
        id,
        UpdateSwapOrderBody.newInstance(success)
    ).wrapErrorMessage()

    internal fun getOutstandingOrders(
        pendingOnly: Boolean
    ) = nabu.getOrders(
        pendingOnly = pendingOnly,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    internal fun getSwapTrades() = nabu.getSwapOrders()

    internal fun getSwapAvailablePairs() =
        nabu.getSwapAvailablePairs()

    internal fun deleteBuyOrder(
        orderId: String
    ) = nabu.deleteBuyOrder(
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
        orderId: String
    ) = nabu.getBuyOrder(
        orderId = orderId,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    fun confirmOrder(
        orderId: String,
        confirmBody: ConfirmOrderRequestBody
    ) = nabu.confirmOrder(
        orderId = orderId,
        confirmBody = confirmBody,
        localisedError = getLocalisedErrorIfEnabled()
    ).wrapErrorMessage()

    fun transferFunds(
        request: TransferRequest
    ): Single<String> = nabu.transferFunds(
        request
    ).map {
        it.id
    }.wrapErrorMessage()

    fun paymentMethods(
        currency: String,
        eligibleOnly: Boolean,
        tier: Int? = null
    ) = nabu.getPaymentMethodsForSimpleBuy(
        currency = currency,
        tier = tier,
        eligibleOnly = eligibleOnly
    ).wrapErrorMessage()

    fun cardAcquirers() = nabu.getCardAcquirers().wrapErrorMessage()

    fun executeTransfer(
        body: ProductTransferRequestBody
    ) = nabu.executeTransfer(
        body = body
    ).wrapErrorMessage()

    fun getRecurringBuyForId(
        recurringBuyId: String
    ) = nabu.getRecurringBuyById(
        recurringBuyId = recurringBuyId
    ).wrapErrorMessage()

    fun cancelRecurringBuy(
        id: String
    ) = nabu.cancelRecurringBuy(
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
