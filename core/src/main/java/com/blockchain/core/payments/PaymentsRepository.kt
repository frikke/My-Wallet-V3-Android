package com.blockchain.core.payments

import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.api.brokerage.data.DepositTermsResponse
import com.blockchain.api.mapActions
import com.blockchain.api.nabu.data.AddressRequest
import com.blockchain.api.paymentmethods.models.AddNewCardBodyRequest
import com.blockchain.api.paymentmethods.models.AliasInfoResponse
import com.blockchain.api.paymentmethods.models.CardProviderResponse
import com.blockchain.api.paymentmethods.models.CardRejectionStateResponse
import com.blockchain.api.paymentmethods.models.CardResponse
import com.blockchain.api.paymentmethods.models.DepositTermsRequestBody
import com.blockchain.api.paymentmethods.models.EveryPayAttrs
import com.blockchain.api.paymentmethods.models.EveryPayCardCredentialsResponse
import com.blockchain.api.paymentmethods.models.Limits
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.api.payments.data.BankInfoResponse
import com.blockchain.api.payments.data.BankMediaResponse
import com.blockchain.api.payments.data.BankMediaResponse.Companion.ICON
import com.blockchain.api.payments.data.BankTransferChargeAttributes
import com.blockchain.api.payments.data.BankTransferChargeResponse
import com.blockchain.api.payments.data.BankTransferPaymentAttributes
import com.blockchain.api.payments.data.BankTransferPaymentBody
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.PLAID_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YAPILY_PARTNER
import com.blockchain.api.payments.data.CreateLinkBankResponse.Companion.YODLEE_PARTNER
import com.blockchain.api.payments.data.LinkBankAttrsResponse
import com.blockchain.api.payments.data.LinkPlaidAccountBody
import com.blockchain.api.payments.data.LinkedBankTransferResponse
import com.blockchain.api.payments.data.OpenBankingTokenBody
import com.blockchain.api.payments.data.PaymentMethodDetailsResponse
import com.blockchain.api.payments.data.ProviderAccountAttrs
import com.blockchain.api.payments.data.RefreshPlaidRequestBody
import com.blockchain.api.payments.data.SettlementBody
import com.blockchain.api.payments.data.UpdateProviderAccountBody
import com.blockchain.api.payments.data.YapilyMediaResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.api.services.PaymentsService
import com.blockchain.api.services.toMobilePaymentType
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.payments.cache.LinkedCardsStore
import com.blockchain.core.payments.cache.PaymentMethodsStore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.domain.common.model.ServerErrorAction
import com.blockchain.domain.common.model.ServerSideUxErrorInfo
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.AliasInfo
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankProviderAccountAttributes
import com.blockchain.domain.paymentmethods.model.BankState
import com.blockchain.domain.paymentmethods.model.BankTransferDetails
import com.blockchain.domain.paymentmethods.model.BankTransferStatus
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardProvider
import com.blockchain.domain.paymentmethods.model.CardRejectionCheckError
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.CardType
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.EligiblePaymentMethodType
import com.blockchain.domain.paymentmethods.model.EveryPayCredentials
import com.blockchain.domain.paymentmethods.model.FundsLock
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.GooglePayInfo
import com.blockchain.domain.paymentmethods.model.InstitutionCountry
import com.blockchain.domain.paymentmethods.model.LinkBankAttributes
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.Partner
import com.blockchain.domain.paymentmethods.model.PartnerCredentials
import com.blockchain.domain.paymentmethods.model.PaymentLimits
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodDetails
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.PaymentMethodTypeWithEligibility
import com.blockchain.domain.paymentmethods.model.PlaidAttributes
import com.blockchain.domain.paymentmethods.model.RefreshBankInfo
import com.blockchain.domain.paymentmethods.model.SettlementInfo
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.domain.paymentmethods.model.YapilyAttributes
import com.blockchain.domain.paymentmethods.model.YapilyInstitution
import com.blockchain.domain.paymentmethods.model.YodleeAttributes
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.common.extensions.wrapErrorMessage
import com.blockchain.nabu.datamanagers.toSupportedPartner
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.fold
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.payments.googlepay.manager.GooglePayManager
import com.blockchain.payments.googlepay.manager.request.GooglePayRequestBuilder
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.store.mapData
import com.blockchain.utils.rxSingleOutcome
import com.blockchain.utils.toZonedDateTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URL
import java.util.Calendar
import java.util.Date

class PaymentsRepository(
    private val paymentsService: PaymentsService,
    private val paymentMethodsStore: PaymentMethodsStore,
    private val paymentMethodsService: PaymentMethodsService,
    private val linkedCardsStore: LinkedCardsStore,
    private val tradingService: TradingService,
    private val assetCatalogue: AssetCatalogue,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val withdrawLocksCache: WithdrawLocksCache,
    private val googlePayManager: GooglePayManager,
    private val environmentConfig: EnvironmentConfig,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val googlePayFeatureFlag: FeatureFlag,
    private val plaidFeatureFlag: FeatureFlag,
) : BankService, CardService, PaymentMethodService {

    private val googlePayEnabled: Single<Boolean> by lazy {
        googlePayFeatureFlag.enabled.cache()
    }

    override suspend fun getPaymentMethodDetailsForIdLegacy(
        paymentId: String,
    ): Outcome<Exception, PaymentMethodDetails> {
        // TODO Turn getAuthHeader() into a suspension function
        return paymentsService.getPaymentMethodDetailsForId(paymentId)
            .map { it.toPaymentDetails() }
    }

    override suspend fun getPaymentMethodDetailsForId(
        paymentId: String,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<PaymentMethodDetails>> {
        return paymentMethodsStore.stream(freshnessStrategy.withKey(PaymentMethodsStore.Key(paymentId)))
            .mapData { it.toPaymentDetails() }
    }

    override fun getWithdrawalLocks(localCurrency: Currency): Single<FundsLocks> =
        withdrawLocksCache.withdrawLocks()
            .map { locks ->
                FundsLocks(
                    onHoldTotalAmount = Money.fromMinor(localCurrency, locks.value.toBigInteger()),
                    locks = locks.locks.map { lock ->
                        FundsLock(
                            amount = Money.fromMinor(localCurrency, lock.value.toBigInteger()),
                            date = lock.date.toZonedDateTime()
                        )
                    }
                )
            }

    override fun getAvailablePaymentMethodsTypes(
        fiatCurrency: FiatCurrency,
        fetchSddLimits: Boolean,
        onlyEligible: Boolean,
    ): Single<List<PaymentMethodTypeWithEligibility>> =
        Single.zip(
            paymentMethodsService.getAvailablePaymentMethodsTypes(
                currency = fiatCurrency.networkTicker,
                tier = if (fetchSddLimits) SDD_ELIGIBLE_TIER else null,
                eligibleOnly = onlyEligible
            ),
            googlePayEnabled,
            rxSingle {
                googlePayManager.checkIfGooglePayIsAvailable(GooglePayRequestBuilder.buildForPaymentStatus())
            }
        ) { methods, isGooglePayFeatureFlagEnabled, isGooglePayAvailableOnDevice ->
            if (isGooglePayFeatureFlagEnabled && isGooglePayAvailableOnDevice) {
                return@zip methods.toMutableList().apply {
                    val googlePayPaymentMethod = this.firstOrNull {
                        it.mobilePayment?.any { payment ->
                            payment.equals(PaymentMethodResponse.GOOGLE_PAY, true)
                        } ?: false
                    }
                    googlePayPaymentMethod?.let {
                        this.add(
                            PaymentMethodResponse(
                                type = PaymentMethodResponse.GOOGLE_PAY,
                                eligible = it.eligible,
                                visible = it.visible,
                                limits = it.limits,
                                subTypes = it.subTypes,
                                currency = it.currency
                            )
                        )
                    }
                }
            } else {
                return@zip methods
            }
        }.map { methods ->
            methods.filter { it.visible }
                .filter { it.eligible || !onlyEligible }
        }.doOnSuccess {
            updateSupportedCards(it)
        }.zipWith(rxSingleOutcome { fiatCurrenciesService.getTradingCurrencies() })
            .map { (methods, tradingCurrencies) ->
                val paymentMethodsTypes = methods
                    .map { it.toAvailablePaymentMethodType(fiatCurrency) }
                    .filterNot { it.type == PaymentMethodType.UNKNOWN }
                    .filter {
                        when (it.type) {
                            PaymentMethodType.BANK_ACCOUNT ->
                                tradingCurrencies.allRecommended.contains(it.currency)
                            PaymentMethodType.FUNDS ->
                                tradingCurrencies.allRecommended.contains(it.currency)
                            PaymentMethodType.BANK_TRANSFER,
                            PaymentMethodType.PAYMENT_CARD,
                            PaymentMethodType.GOOGLE_PAY,
                            PaymentMethodType.UNKNOWN,
                            -> true
                        }
                    }

                paymentMethodsTypes
            }

    override fun getEligiblePaymentMethodTypes(fiatCurrency: FiatCurrency): Single<List<EligiblePaymentMethodType>> =
        getAvailablePaymentMethodsTypes(
            fiatCurrency = fiatCurrency,
            fetchSddLimits = false,
            onlyEligible = true
        ).map { available ->
            available.map { EligiblePaymentMethodType(it.type, it.currency) }
        }

    private fun updateSupportedCards(types: List<PaymentMethodResponse>) {
        val cardTypes =
            types
                .asSequence()
                .filter { it.eligible && it.type.toPaymentMethodType() == PaymentMethodType.PAYMENT_CARD }
                .filter { it.subTypes.isNullOrEmpty().not() }
                .mapNotNull { it.subTypes }
                .flatten().distinct()
                .toList()
        simpleBuyPrefs.updateSupportedCards(cardTypes.joinToString())
    }

    override fun getLinkedPaymentMethods(
        currency: FiatCurrency,
    ): Single<List<LinkedPaymentMethod>> =
        Single.zip(
            tradingService.getBalanceFor(currency).firstOrError(),
            getLinkedCards(),
            paymentMethodsService.getBanks().onErrorReturn { emptyList() }
        ) { fundsResponse, cards, linkedBanks ->
            val funds = listOf(LinkedPaymentMethod.Funds(fundsResponse.total, currency))
            val banks = linkedBanks.mapNotNull { it.toPaymentMethod() }

            (cards + funds + banks)
        }

    override fun getLinkedCards(
        request: FreshnessStrategy,
        vararg states: CardStatus,
    ): Flow<DataResource<List<LinkedPaymentMethod.Card>>> =
        linkedCardsStore.stream(request)
            .mapData {
                it.filter { states.contains(it.state.toCardStatus()) || states.isEmpty() }
                    .map { item -> item.toPaymentMethod() }
            }

    override fun getLinkedCards(
        vararg states: CardStatus,
    ): Single<List<LinkedPaymentMethod.Card>> =
        rxSingleOutcome {
            getLinkedCards(FreshnessStrategy.Fresh, *states).firstOutcome()
        }

    override fun getLinkedBank(id: String): Single<LinkedBank> =
        paymentMethodsService.getLinkedBank(
            id = id
        ).map {
            it.toDomainOrThrow()
        }

    private fun LinkedBankTransferResponse.toDomainOrThrow() =
        ux?.let {
            throw NabuApiExceptionFactory.fromServerSideError(it)
        } ?: toLinkedBank()

    override fun addNewCard(
        fiatCurrency: FiatCurrency,
        billingAddress: BillingAddress,
        paymentMethodTokens: Map<String, String>?,
    ): Single<CardToBeActivated> =
        paymentMethodsService.addNewCard(
            addNewCardBodyRequest = AddNewCardBodyRequest(
                fiatCurrency.networkTicker,
                billingAddress.toAddressRequest(),
                paymentMethodTokens
            )
        ).map {
            CardToBeActivated(cardId = it.id, partner = it.partner.toPartner())
        }.doOnSuccess {
            linkedCardsStore.markAsStale()
        }.wrapErrorMessage()

    override fun activateCard(cardId: String, redirectUrl: String, cvv: String): Single<PartnerCredentials> =
        paymentMethodsService.activateCard(
            cardId,
            SimpleBuyConfirmationAttributes(
                everypay = EveryPayAttrs(redirectUrl),
                redirectURL = redirectUrl,
                cvv = cvv
            )
        ).map { response ->
            // Either everypay or cardProvider will be provided by ActivateCardResponse, never both
            when {
                response.everypay != null -> PartnerCredentials.EverypayPartner(
                    everyPay = response.everypay!!.toEverypayCredentials()
                )
                response.cardProvider != null -> PartnerCredentials.CardProviderPartner(
                    cardProvider = response.cardProvider!!.toCardProvider()
                )
                else -> PartnerCredentials.Unknown
            }
        }.doOnSuccess {
            linkedCardsStore.markAsStale()
        }.wrapErrorMessage()

    override fun getCardDetails(cardId: String): Single<PaymentMethod.Card> =
        paymentMethodsService.getCardDetails(cardId).map { cardsResponse ->
            cardsResponse.toPaymentMethod().toCardPaymentMethod(
                cardLimits = PaymentLimits(
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    FiatCurrency.fromCurrencyCode(cardsResponse.currency)
                )
            )
        }

    override fun getGooglePayTokenizationParameters(currency: String): Single<GooglePayInfo> =
        paymentMethodsService.getGooglePayInfo(currency).map {
            GooglePayInfo(
                beneficiaryID = it.beneficiaryID,
                merchantBankCountryCode = it.merchantBankCountryCode,
                googlePayParameters = it.googlePayParameters,
                publishableApiKey = it.publishableApiKey,
                allowPrepaidCards = it.allowPrepaidCards,
                allowCreditCards = it.allowCreditCards,
                allowedAuthMethods = it.allowedAuthMethods,
                allowedCardNetworks = it.allowedCardNetworks,
                billingAddressRequired = it.billingAddressRequired,
                billingAddressParameters = GooglePayInfo.BillingAddressParameters(
                    format = it.billingAddressParameters?.format,
                    phoneNumberRequired = it.billingAddressParameters?.phoneNumberRequired
                )
            )
        }

    override fun deleteCard(cardId: String): Completable =
        paymentMethodsService.deleteCard(cardId).doOnComplete {
            linkedCardsStore.markAsStale()
        }

    override fun getLinkedBanks(): Single<List<LinkedPaymentMethod.Bank>> {
        return paymentMethodsService.getBanks()
            .map { banksResponse ->
                banksResponse.mapNotNull { it.toPaymentMethod() }
            }
    }

    override fun removeBank(bank: LinkedPaymentMethod.Bank): Completable =
        when (bank.type) {
            PaymentMethodType.BANK_ACCOUNT -> paymentMethodsService.removeBeneficiary(bank.id)
            PaymentMethodType.BANK_TRANSFER -> paymentMethodsService.removeLinkedBank(bank.id)
            else -> Completable.error(IllegalStateException("Unknown Bank type"))
        }

    override fun linkBank(currency: FiatCurrency): Single<LinkBankTransfer> =
        plaidFeatureFlag.enabled.flatMap { featureFlag ->
            val supportedPartners = if (featureFlag)
                listOf(PLAID_PARTNER, YODLEE_PARTNER, YAPILY_PARTNER)
            else
                emptyList()

            paymentMethodsService.linkBank(
                currency.networkTicker,
                supportedPartners,
                environmentConfig.applicationId
            )
        }.flatMap { response ->
            val partner =
                response.partner.toLinkingBankPartner() ?: return@flatMap Single.error<LinkBankTransfer>(
                    IllegalStateException("Partner not Supported")
                )
            val attributes =
                response.attributes ?: return@flatMap Single.error<LinkBankTransfer>(
                    IllegalStateException("Missing attributes")
                )
            Single.just(
                LinkBankTransfer(
                    response.id,
                    partner,
                    partner.attributes(attributes)
                )
            )
        }.wrapErrorMessage()

    fun BankPartner.attributes(attrsResponse: LinkBankAttrsResponse): LinkBankAttributes =
        when (this) {
            BankPartner.YODLEE -> {
                YodleeAttributes(
                    attrsResponse.fastlinkUrl!!,
                    attrsResponse.token!!,
                    attrsResponse.fastlinkParams!!.configName
                )
            }
            BankPartner.YAPILY -> {
                YapilyAttributes(
                    entity = attrsResponse.entity!!,
                    institutionList = attrsResponse.institutions!!.map {
                        YapilyInstitution(
                            operatingCountries = it.countries.map { countryResponse ->
                                InstitutionCountry(
                                    countryCode = countryResponse.countryCode2,
                                    displayName = countryResponse.displayName
                                )
                            },
                            name = it.fullName,
                            id = it.id,
                            iconLink = it.media.getBankIcon()
                        )
                    }
                )
            }
            BankPartner.PLAID ->
                PlaidAttributes(
                    linkToken = attrsResponse.linkToken!!,
                    linkUrl = attrsResponse.linkUrl!!,
                    tokenExpiresAt = attrsResponse.tokenExpiresAt!!
                )
        }

    private fun List<YapilyMediaResponse>.getBankIcon(): URL? =
        try {
            URL(find { it.type == BankPartner.ICON }?.source)
        } catch (e: MalformedURLException) {
            null
        }

    override fun startBankTransfer(
        id: String,
        amount: Money,
        currency: String,
        callback: String?,
    ): Single<String> =
        paymentMethodsService.startBankTransferPayment(
            id = id,
            body = BankTransferPaymentBody(
                amountMinor = amount.toBigInteger().toString(),
                currency = currency,
                product = "SIMPLEBUY",
                attributes = if (callback != null) {
                    BankTransferPaymentAttributes(callback)
                } else null
            )
        ).map {
            it.paymentId
        }

    override fun updateSelectedBankAccount(
        linkingId: String,
        providerAccountId: String,
        accountId: String,
        attributes: BankProviderAccountAttributes,
    ): Completable = paymentMethodsService.updateAccountProviderId(
        linkingId,
        UpdateProviderAccountBody(attributes.toProviderAttributes())
    )

    override fun linkPlaidBankAccount(
        linkingId: String,
        accountId: String,
        publicToken: String,
    ): Completable = paymentMethodsService.linkPLaidAccount(
        linkingId,
        LinkPlaidAccountBody(LinkPlaidAccountBody.Attributes(accountId, publicToken))
    )

    override fun refreshPlaidBankAccount(refreshAccountId: String): Single<RefreshBankInfo> =
        paymentMethodsService.refreshPlaidAccount(
            refreshAccountId,
            RefreshPlaidRequestBody(packageName = environmentConfig.applicationId)
        ).map {
            RefreshBankInfo(
                id = it.id,
                partner = it.partner.toLinkingBankPartner(),
                linkToken = it.attributes.linkToken,
                linkUrl = it.attributes.linkUrl,
                tokenExpiresAt = it.attributes.tokenExpiresAt
            )
        }.wrapErrorMessage()

    override fun checkSettlement(accountId: String, amount: Money): Single<SettlementInfo> =
        paymentMethodsService.checkSettlement(
            accountId,
            SettlementBody(
                SettlementBody.Attributes(
                    SettlementBody.Attributes.SettlementRequest(
                        amount = amount.toNetworkString()
                    )
                )
            )
        ).map {
            SettlementInfo(
                partner = it.partner.toLinkingBankPartner(),
                state = it.state.toBankState(),
                settlementType = it.attributes?.settlementResponse?.settlementType?.toSettlementType()
                    ?: SettlementType.UNKNOWN,
                settlementReason = it.attributes?.settlementResponse?.reason?.toSettlementReason()
                    ?: SettlementReason.NONE
            )
        }

    override suspend fun getDepositTerms(paymentMethodId: String, amount: Money): Outcome<Exception, DepositTerms> =
        paymentMethodsService.getDepositTerms(
            DepositTermsRequestBody(
                amount = DepositTermsRequestBody.Amount(
                    value = amount.toBigInteger().toString(),
                    currency = amount.currencyCode
                ),
                paymentMethodId = paymentMethodId
            )
        ).fold(
            onSuccess = {
                Outcome.Success(it.toDepositTerms())
            },
            onFailure = {
                Outcome.Failure(it)
            }
        )

    private fun BankProviderAccountAttributes.toProviderAttributes() =
        ProviderAccountAttrs(
            providerAccountId = providerAccountId,
            accountId = accountId,
            institutionId = institutionId,
            callback = callback
        )

    override fun updateOpenBankingConsent(
        url: String,
        token: String,
    ): Completable =
        paymentMethodsService.updateOpenBankingToken(
            url,
            OpenBankingTokenBody(
                oneTimeToken = token
            )
        )

    override fun getBankTransferCharge(paymentId: String): Single<BankTransferDetails> =
        paymentMethodsService.getBankTransferCharge(
            paymentId = paymentId
        ).map {
            it.toDomainOrThrow()
        }

    private fun BankTransferChargeResponse.toDomainOrThrow() =
        ux?.let {
            throw NabuApiExceptionFactory.fromServerSideError(it)
        } ?: toBankTransferDetails()

    override fun canTransactWithBankMethods(fiatCurrency: FiatCurrency): Single<Boolean> =
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            fiatCurrenciesService.getTradingCurrencies()
        }.flatMap { tradingCurrencies ->
            if (!tradingCurrencies.allRecommended.contains(fiatCurrency)) {
                Single.just(false)
            } else {
                getAvailablePaymentMethodsTypes(
                    fiatCurrency = fiatCurrency,
                    fetchSddLimits = false,
                    onlyEligible = true
                ).map { available ->
                    available.any {
                        it.type == PaymentMethodType.BANK_ACCOUNT || it.type == PaymentMethodType.BANK_TRANSFER
                    }
                }
            }
        }

    override suspend fun getBeneficiaryInfo(currency: String, address: String): Outcome<Exception, AliasInfo> =
        paymentMethodsService.getBeneficiaryInfo(
            currency = currency,
            address = address
        ).fold(
            onSuccess = {
                it.ux?.let { error ->
                    Outcome.Failure(NabuApiExceptionFactory.fromServerSideError(error))
                } ?: Outcome.Success(it.toAliasInfo())
            },
            onFailure = {
                Outcome.Failure(it)
            }
        )

    override suspend fun activateBeneficiary(beneficiaryId: String): Outcome<Exception, Unit> =
        paymentMethodsService.activateBeneficiary(
            beneficiaryId = beneficiaryId
        )

    override suspend fun checkNewCardRejectionState(binNumber: String):
        Outcome<CardRejectionCheckError, CardRejectionState> =
        paymentMethodsService.checkCardRejectionState(
            binNumber
        ).map { response ->
            response.toDomain()
        }.mapError {
            CardRejectionCheckError.REQUEST_FAILED
        }

    private fun CardRejectionStateResponse.toDomain(): CardRejectionState =
        when (this.block) {
            true -> {
                CardRejectionState.AlwaysRejected(
                    errorId = this.ux?.id,
                    title = this.ux?.title,
                    description = this.ux?.message,
                    actions = this.ux?.actions.takeUnless { it.isNullOrEmpty() }?.map { data ->
                        ServerErrorAction(
                            title = data.title,
                            deeplinkPath = data.url.orEmpty()
                        )
                    }.orEmpty(),
                    iconUrl = this.ux?.icon?.url,
                    statusIconUrl = this.ux?.icon?.status?.url,
                    analyticsCategories = this.ux?.categories.orEmpty()
                )
            }
            false -> {
                if (this.ux?.actions?.isNotEmpty() == true) {
                    CardRejectionState.MaybeRejected(
                        errorId = this.ux?.id,
                        title = this.ux?.title,
                        description = this.ux?.message,
                        actions = this.ux?.actions.takeUnless { it.isNullOrEmpty() }?.map { data ->
                            ServerErrorAction(
                                title = data.title,
                                deeplinkPath = data.url.orEmpty()
                            )
                        }.orEmpty(),
                        iconUrl = this.ux?.icon?.url,
                        statusIconUrl = this.ux?.icon?.status?.url,
                        analyticsCategories = this.ux?.categories.orEmpty()
                    )
                } else {
                    CardRejectionState.NotRejected
                }
            }
        }

    private fun CardResponse.toPaymentMethod(): LinkedPaymentMethod.Card =
        LinkedPaymentMethod.Card(
            cardId = id,
            label = card?.label.orEmpty(),
            endDigits = card?.number.orEmpty(),
            partner = partner.toSupportedPartner(),
            expireDate = card?.let {
                Calendar.getInstance().apply {
                    set(
                        it.expireYear ?: this.get(Calendar.YEAR),
                        it.expireMonth ?: this.get(Calendar.MONTH),
                        0
                    )
                }.time
            } ?: Date(),
            cardType = card?.type?.toCardType() ?: CardType.UNKNOWN,
            status = state.toCardStatus(),
            currency = assetCatalogue.fiatFromNetworkTicker(currency)
                ?: throw IllegalStateException("Unknown currency $currency"),
            mobilePaymentType = mobilePaymentType?.toMobilePaymentType(),
            cardRejectionState = CardRejectionStateResponse(block, ux).toDomain(),
            serverSideUxErrorInfo = ux?.let {
                ServerSideUxErrorInfo(
                    id = it.id,
                    title = it.title,
                    description = it.message,
                    iconUrl = it.icon?.url.orEmpty(),
                    statusUrl = it.icon?.status?.url.orEmpty(),
                    actions = it.mapActions(),
                    categories = it.categories ?: emptyList()
                )
            }
        )

    private fun LinkedPaymentMethod.Card.toCardPaymentMethod(cardLimits: PaymentLimits) =
        PaymentMethod.Card(
            cardId = cardId,
            limits = cardLimits,
            label = label,
            endDigits = endDigits,
            partner = partner,
            expireDate = expireDate,
            cardType = cardType,
            status = status,
            isEligible = true,
            mobilePaymentType = mobilePaymentType,
            serverSideUxErrorInfo = serverSideUxErrorInfo
        )

    private fun BankInfoResponse.toPaymentMethod(): LinkedPaymentMethod.Bank? {
        return LinkedPaymentMethod.Bank(
            id = id,
            name = name.takeIf { it?.isNotEmpty() == true } ?: accountName.orEmpty(),
            accountEnding = accountNumber ?: "****",
            accountType = bankAccountType.orEmpty(),
            iconUrl = attributes?.media?.find { it.type == BankMediaResponse.ICON }?.source.orEmpty(),
            isBankTransferAccount = isBankTransferAccount,
            state = state.toBankState(),
            currency = assetCatalogue.fiatFromNetworkTicker(currency) ?: return null
        )
    }

    private fun LinkedBankTransferResponse.toLinkedBank(): LinkedBank? {
        val bankPartner = partner.toLinkingBankPartner() ?: return null
        return LinkedBank(
            id = id,
            currency = assetCatalogue.fiatFromNetworkTicker(currency) ?: return null,
            partner = bankPartner,
            state = state.toLinkedBankState(),
            bankName = details?.bankName.orEmpty(),
            accountName = details?.accountName.orEmpty(),
            accountNumber = details?.accountNumber?.replace("x", "").orEmpty(),
            errorStatus = error?.toLinkedBankErrorState() ?: LinkedBankErrorState.NONE,
            accountType = details?.bankAccountType.orEmpty(),
            authorisationUrl = attributes?.authorisationUrl.orEmpty(),
            sortCode = details?.sortCode.orEmpty(),
            accountIban = details?.iban.orEmpty(),
            bic = details?.bic.orEmpty(),
            entity = attributes?.entity.orEmpty(),
            iconUrl = attributes?.media?.find { it.source == ICON }?.source.orEmpty(),
            callbackPath = if (bankPartner == BankPartner.YAPILY) {
                attributes?.callbackPath ?: throw IllegalArgumentException("Missing callbackPath")
            } else {
                attributes?.callbackPath.orEmpty()
            }

        )
    }

    private fun String.toLinkedBankState(): LinkedBankState =
        when (this) {
            LinkedBankTransferResponse.CREATED -> LinkedBankState.CREATED
            LinkedBankTransferResponse.ACTIVE -> LinkedBankState.ACTIVE
            LinkedBankTransferResponse.PENDING,
            LinkedBankTransferResponse.FRAUD_REVIEW,
            LinkedBankTransferResponse.MANUAL_REVIEW,
            -> LinkedBankState.PENDING
            LinkedBankTransferResponse.BLOCKED -> LinkedBankState.BLOCKED
            else -> LinkedBankState.UNKNOWN
        }

    private fun String.toLinkedBankErrorState(): LinkedBankErrorState =
        when (this) {
            LinkedBankTransferResponse.ERROR_ALREADY_LINKED -> LinkedBankErrorState.ACCOUNT_ALREADY_LINKED
            LinkedBankTransferResponse.ERROR_ACCOUNT_INFO_NOT_FOUND -> LinkedBankErrorState.NOT_INFO_FOUND
            LinkedBankTransferResponse.ERROR_ACCOUNT_NOT_SUPPORTED -> LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED
            LinkedBankTransferResponse.ERROR_NAMES_MISMATCHED -> LinkedBankErrorState.NAMES_MISMATCHED
            LinkedBankTransferResponse.ERROR_ACCOUNT_EXPIRED -> LinkedBankErrorState.EXPIRED
            LinkedBankTransferResponse.ERROR_ACCOUNT_REJECTED -> LinkedBankErrorState.REJECTED
            LinkedBankTransferResponse.ERROR_ACCOUNT_FAILURE -> LinkedBankErrorState.FAILURE
            LinkedBankTransferResponse.ERROR_ACCOUNT_INVALID -> LinkedBankErrorState.INVALID
            LinkedBankTransferResponse.ERROR_ACCOUNT_FAILED_INTERNAL -> LinkedBankErrorState.INTERNAL_FAILURE
            LinkedBankTransferResponse.ERROR_ACCOUNT_REJECTED_FRAUD -> LinkedBankErrorState.FRAUD
            else -> LinkedBankErrorState.UNKNOWN
        }

    private fun String.toLinkingBankPartner(): BankPartner? {
        val partner = when (this) {
            YODLEE_PARTNER -> BankPartner.YODLEE
            YAPILY_PARTNER -> BankPartner.YAPILY
            PLAID_PARTNER -> BankPartner.PLAID
            else -> null
        }

        return if (SUPPORTED_BANK_PARTNERS.contains(partner)) {
            partner
        } else null
    }

    private fun String.toSettlementReason(): SettlementReason = try {
        SettlementReason.valueOf(this)
    } catch (ex: Exception) {
        SettlementReason.UNKNOWN
    }

    private fun String.toSettlementType(): SettlementType = try {
        SettlementType.valueOf(this)
    } catch (ex: Exception) {
        SettlementType.UNKNOWN
    }

    private fun Limits.toPaymentLimits(currency: FiatCurrency): PaymentLimits = PaymentLimits(
        min.toBigInteger(),
        max.toBigInteger(),
        currency
    )

    private fun String.toBankState(): BankState =
        when (this) {
            BankInfoResponse.ACTIVE -> BankState.ACTIVE
            BankInfoResponse.PENDING -> BankState.PENDING
            BankInfoResponse.BLOCKED -> BankState.BLOCKED
            else -> BankState.UNKNOWN
        }

    private fun String.toCardStatus(): CardStatus =
        when (this) {
            CardResponse.ACTIVE -> CardStatus.ACTIVE
            CardResponse.BLOCKED -> CardStatus.BLOCKED
            CardResponse.PENDING -> CardStatus.PENDING
            CardResponse.CREATED -> CardStatus.CREATED
            CardResponse.EXPIRED -> CardStatus.EXPIRED
            else -> CardStatus.UNKNOWN
        }

    private fun String.toPaymentMethodType(): PaymentMethodType = when (this) {
        PaymentMethodResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
        PaymentMethodResponse.FUNDS -> PaymentMethodType.FUNDS
        PaymentMethodResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
        PaymentMethodResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
        PaymentMethodResponse.GOOGLE_PAY -> PaymentMethodType.GOOGLE_PAY
        else -> PaymentMethodType.UNKNOWN
    }

    private fun PaymentMethodResponse.toAvailablePaymentMethodType(
        currency: FiatCurrency,
    ): PaymentMethodTypeWithEligibility =
        PaymentMethodTypeWithEligibility(
            eligible = eligible,
            currency = currency,
            type = type.toPaymentMethodType(),
            limits = limits.toPaymentLimits(currency),
            cardFundSources = cardFundSources
        )

    private fun BankTransferChargeResponse.toBankTransferDetails() =
        BankTransferDetails(
            id = this.beneficiaryId,
            amount = Money.fromMinor(
                assetCatalogue.fromNetworkTicker(amount.symbol) ?: throw IllegalArgumentException(
                    "Currency not supported"
                ),
                this.amountMinor.toBigInteger()
            ),
            authorisationUrl = this.extraAttributes?.authorisationUrl,
            status = this.state?.toBankTransferStatus(this.error)
                ?: this.extraAttributes?.status?.toBankTransferStatus(this.error)
                ?: BankTransferStatus.Unknown
        )

    private fun String.toBankTransferStatus(error: String?) =
        when (this) {
            BankTransferChargeAttributes.CREATED,
            BankTransferChargeAttributes.PRE_CHARGE_REVIEW,
            BankTransferChargeAttributes.PRE_CHARGE_APPROVED,
            BankTransferChargeAttributes.AWAITING_AUTHORIZATION,
            BankTransferChargeAttributes.PENDING,
            BankTransferChargeAttributes.AUTHORIZED,
            BankTransferChargeAttributes.CREDITED,
            -> BankTransferStatus.Pending
            BankTransferChargeAttributes.FAILED,
            BankTransferChargeAttributes.FRAUD_REVIEW,
            BankTransferChargeAttributes.MANUAL_REVIEW,
            BankTransferChargeAttributes.REJECTED,
            -> BankTransferStatus.Error(error)
            BankTransferChargeAttributes.CLEARED,
            BankTransferChargeAttributes.COMPLETE,
            -> BankTransferStatus.Complete
            else -> BankTransferStatus.Unknown
        }

    private fun BillingAddress.toAddressRequest() = AddressRequest(
        line1 = addressLine1,
        line2 = addressLine2,
        city = city,
        countryCode = countryCode,
        postCode = postCode,
        state = state
    )

    private fun EveryPayCardCredentialsResponse.toEverypayCredentials() =
        EveryPayCredentials(
            apiUsername,
            mobileToken,
            paymentLink
        )

    private fun CardProviderResponse.toCardProvider() =
        CardProvider(
            cardAcquirerName,
            cardAcquirerAccountCode,
            apiUserID.orEmpty(),
            apiToken.orEmpty(),
            paymentLink.orEmpty(),
            paymentState.orEmpty(),
            paymentReference.orEmpty(),
            orderReference.orEmpty(),
            clientSecret.orEmpty(),
            publishableApiKey.orEmpty()
        )

    private fun String.toPartner(): Partner = when (this) {
        "EVERYPAY" -> Partner.EVERYPAY
        "CARDPROVIDER" -> Partner.CARDPROVIDER
        else -> Partner.UNKNOWN
    }

    private fun AliasInfoResponse.toAliasInfo(): AliasInfo =
        AliasInfo(
            bankName = agent?.bankName,
            alias = agent?.label,
            accountHolder = agent?.name,
            accountType = agent?.accountType,
            cbu = agent?.address,
            cuil = agent?.holderDocument
        )

    private fun DepositTermsResponse.toDepositTerms(): DepositTerms =
        DepositTerms(
            creditCurrency = this.creditCurrency,
            availableToTradeMinutesMin = this.availableToTradeMinutesMin,
            availableToTradeMinutesMax = this.availableToTradeMinutesMax,
            availableToTradeDisplayMode = this.availableToTradeDisplayMode.toDisplayMode(),
            availableToWithdrawMinutesMin = this.availableToWithdrawMinutesMin,
            availableToWithdrawMinutesMax = this.availableToWithdrawMinutesMax,
            availableToWithdrawDisplayMode = this.availableToWithdrawDisplayMode.toDisplayMode(),
            settlementType = this.settlementType?.toSettlementType(),
            settlementReason = this.settlementReason?.toSettlementReason()
        )

    private fun String.toDisplayMode(): DepositTerms.DisplayMode = try {
        DepositTerms.DisplayMode.valueOf(this)
    } catch (ex: Exception) {
        DepositTerms.DisplayMode.NONE
    }

    // </editor-fold>

    companion object {
        private val SUPPORTED_BANK_PARTNERS = listOf(
            BankPartner.YAPILY, BankPartner.YODLEE, BankPartner.PLAID
        )
        private const val SDD_ELIGIBLE_TIER = 3
    }
}

private fun String.toCardType(): CardType = try {
    CardType.valueOf(this)
} catch (ex: Exception) {
    CardType.UNKNOWN
}

private fun PaymentMethodDetailsResponse.toPaymentDetails(): PaymentMethodDetails {
    return when (this.paymentMethodType) {
        PaymentMethodDetailsResponse.PAYMENT_CARD -> {
            PaymentMethodDetails(
                label = cardDetails?.card?.label,
                endDigits = cardDetails?.card?.number,
                mobilePaymentType = cardDetails?.mobilePaymentType?.toMobilePaymentType()
            )
        }
        PaymentMethodDetailsResponse.BANK_TRANSFER -> {
            check(bankTransferAccountDetails != null) { "bankTransferAccountDetails not present" }
            bankTransferAccountDetails!!.let { bankTransferAccountDetails ->
                check(bankTransferAccountDetails.details != null) { "bankTransferAccountDetails not present" }
                bankTransferAccountDetails.details!!.let { detail ->
                    PaymentMethodDetails(
                        label = detail.accountName,
                        endDigits = detail.accountNumber
                    )
                }
            }
        }
        PaymentMethodDetailsResponse.BANK_ACCOUNT -> {
            check(bankAccountDetails != null) { "bankAccountDetails not present" }
            bankAccountDetails!!.let { bankAccountDetails ->
                check(bankAccountDetails.extraAttributes != null) { "extraAttributes not present" }
                bankAccountDetails.extraAttributes!!.let { extraAttributes ->
                    PaymentMethodDetails(
                        label = extraAttributes.name,
                        endDigits = extraAttributes.address
                    )
                }
            }
        }
        else -> PaymentMethodDetails()
    }
}