package piuk.blockchain.android.simplebuy

import com.blockchain.analytics.Analytics
import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.banking.BankTransferAction
import com.blockchain.coincore.Coincore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.GetRegionScope
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.BankProviderAccountAttributes
import com.blockchain.domain.paymentmethods.model.BillingAddress
import com.blockchain.domain.paymentmethods.model.CardRejectionState
import com.blockchain.domain.paymentmethods.model.CardStatus
import com.blockchain.domain.paymentmethods.model.CardToBeActivated
import com.blockchain.domain.paymentmethods.model.EligiblePaymentMethodType
import com.blockchain.domain.paymentmethods.model.LegacyLimits
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedPaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentCardAcquirer
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.service.TierService
import com.blockchain.network.PollResult
import com.blockchain.network.PollService
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.getOrThrow
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardBillingAddress
import com.blockchain.payments.core.CardDetails
import com.blockchain.payments.core.CardProcessor
import com.blockchain.payments.core.PaymentToken
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.serializers.StringMapSerializer
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.serialization.json.Json
import piuk.blockchain.android.cards.CardData
import piuk.blockchain.android.cards.CardIntent
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.rating.domain.model.APP_RATING_MINIMUM_BUY_ORDERS
import piuk.blockchain.android.sdd.SDDAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.linkbank.BankLinkingInfo
import piuk.blockchain.android.ui.linkbank.fromPreferencesValue
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome
import timber.log.Timber

class SimpleBuyInteractor(
    private val tierService: TierService,
    private val custodialWalletManager: CustodialWalletManager,
    private val limitsDataManager: LimitsDataManager,
    private val withdrawLocksRepository: WithdrawLocksRepository,
    private val analytics: Analytics,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val eligibilityProvider: SimpleBuyEligibilityProvider,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
    private val coincore: Coincore,
    private val userIdentity: UserIdentity,
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val cardProcessors: Map<CardAcquirer, CardProcessor>,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase,
    private val bankService: BankService,
    private val cardService: CardService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentsRepository: PaymentsRepository,
    private val quickFillButtonsFeatureFlag: FeatureFlag,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val onboardingPrefs: OnboardingPrefs,
    private val cardRejectionCheckFF: FeatureFlag,
    private val eligibilityService: EligibilityService,
) {

    // Hack until we have a proper limits api.
    // ignore limits when user is in tier BRONZE. When user is in tier BRONZE available limit for transfer is 0, so
    // the user will never be able to continue the flow. That's why we don't restrict him. PaymentMethod limit will
    // restrict him only.

    fun fetchBuyLimits(
        fiat: FiatCurrency,
        asset: AssetInfo,
        paymentMethodType: PaymentMethodType
    ): Single<TxLimits> =
        Singles.zip(
            fetchLimits(sourceCurrency = fiat, targetCurrency = asset, paymentMethodType = paymentMethodType),
            userIdentity.getHighestApprovedKycTier()
        ).flatMap { (limits, highestTier) ->
            when (highestTier) {
                Tier.BRONZE -> Single.just(limits.copy(max = TxLimit.Unlimited))
                Tier.SILVER -> userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence).map { isSdd ->
                    if (isSdd) limits
                    else limits.copy(max = TxLimit.Unlimited)
                }
                Tier.GOLD -> Single.just(limits)
            }
        }

    private fun fetchLimits(
        targetCurrency: AssetInfo,
        sourceCurrency: FiatCurrency,
        paymentMethodType: PaymentMethodType
    ): Single<TxLimits> {
        return limitsDataManager.getLimits(
            outputCurrency = sourceCurrency,
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            targetAccountType = AssetCategory.CUSTODIAL,
            sourceAccountType = if (paymentMethodType == PaymentMethodType.FUNDS) {
                AssetCategory.CUSTODIAL
            } else {
                AssetCategory.NON_CUSTODIAL
            },
            legacyLimits = custodialWalletManager.getProductTransferLimits(
                currency = sourceCurrency,
                product = Product.BUY
            ).map { it as LegacyLimits }
        )
    }

    fun cancelOrder(orderId: String): Completable = cancelOrderUseCase.invoke(orderId)

    fun createRecurringBuyOrder(
        asset: AssetInfo?,
        order: SimpleBuyOrder,
        selectedPaymentMethod: SelectedPaymentMethod?,
        recurringBuyFrequency: RecurringBuyFrequency
    ): Single<RecurringBuyOrder> {
        return if (recurringBuyFrequency != RecurringBuyFrequency.ONE_TIME) {
            require(asset != null) { "createRecurringBuyOrder selected crypto is null" }
            require(order.amount != null) { "createRecurringBuyOrder amount is null" }
            require(selectedPaymentMethod != null) { "createRecurringBuyOrder selected payment method is null" }

            val amount = order.amount
            custodialWalletManager.createRecurringBuyOrder(
                RecurringBuyRequestBody(
                    inputValue = amount.toBigInteger().toString(),
                    inputCurrency = amount.currencyCode,
                    destinationCurrency = asset.networkTicker,
                    paymentMethod = selectedPaymentMethod.paymentMethodType.name,
                    period = recurringBuyFrequency.name,
                    paymentMethodId = selectedPaymentMethod.takeUnless { it.isFunds() }?.id
                )
            )
        } else {
            Single.just(RecurringBuyOrder())
        }
    }

    fun fetchWithdrawLockTime(
        paymentMethod: PaymentMethodType,
        fiatCurrency: FiatCurrency
    ): Single<SimpleBuyIntent.WithdrawLocksTimeUpdated> =
        withdrawLocksRepository.getWithdrawLockTypeForPaymentMethod(paymentMethod, fiatCurrency)
            .map {
                SimpleBuyIntent.WithdrawLocksTimeUpdated(it)
            }.onErrorReturn {
                SimpleBuyIntent.WithdrawLocksTimeUpdated()
            }

    fun pollForKycState(): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers()
            .flatMap {
                when {
                    it.isApprovedFor(KycTierLevel.GOLD) ->
                        eligibilityProvider.isEligibleForSimpleBuy(forceRefresh = true).map { eligible ->
                            if (eligible) {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                            } else {
                                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                            }
                        }
                    it.isRejectedForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                    it.isInReviewForAny() -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW))
                    else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
                }
            }.onErrorReturn {
                SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
            }
            .repeatWhen { it.delay(INTERVAL, TimeUnit.SECONDS).zipWith(Flowable.range(0, RETRIES_SHORT)) }
            .takeUntil { it.kycState != KycState.PENDING }
            .last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            .map {
                if (it.kycState == KycState.PENDING) {
                    SimpleBuyIntent.KycStateUpdated(KycState.UNDECIDED)
                } else {
                    it
                }
            }

    fun updateSelectedBankAccountId(
        linkingId: String,
        providerAccountId: String = "",
        accountId: String,
        partner: BankPartner,
        action: BankTransferAction,
        source: BankAuthSource
    ): Completable {
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(
                bankAuthFlow = BankAuthFlowState.BANK_LINK_PENDING,
                bankLinkingInfo = BankLinkingInfo(linkingId, source)
            ).toPreferencesValue()
        )

        return bankService.updateSelectedBankAccount(
            linkingId = linkingId,
            providerAccountId = providerAccountId,
            accountId = accountId,
            attributes = providerAttributes(
                partner = partner,
                action = action,
                providerAccountId = providerAccountId,
                accountId = accountId
            )
        )
    }

    private fun providerAttributes(
        partner: BankPartner,
        providerAccountId: String,
        accountId: String,
        action: BankTransferAction
    ): BankProviderAccountAttributes =
        when (partner) {
            BankPartner.YODLEE, BankPartner.PLAID ->
                BankProviderAccountAttributes(
                    providerAccountId = providerAccountId,
                    accountId = accountId
                )
            BankPartner.YAPILY ->
                BankProviderAccountAttributes(
                    institutionId = accountId,
                    callback = bankPartnerCallbackProvider.callback(BankPartner.YAPILY, action)
                )
        }

    fun pollForBankLinkingCompleted(id: String): Single<LinkedBank> = PollService(
        bankService.getLinkedBank(id)
    ) {
        it.isLinkingInFinishedState()
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT).map {
        it.value
    }

    fun pollForLinkedBankState(id: String, partner: BankPartner?): Single<PollResult<LinkedBank>> = PollService(
        bankService.getLinkedBank(id)
    ) {
        if (partner == BankPartner.YAPILY) {
            it.authorisationUrl.isNotEmpty() && it.callbackPath.isNotEmpty()
        } else {
            !it.isLinkingPending()
        }
    }.start(timerInSec = INTERVAL, retries = RETRIES_DEFAULT)

    fun checkTierLevel(): Single<SimpleBuyIntent.KycStateUpdated> {
        return tierService.tiers().flatMap {
            when {
                it.isApprovedFor(KycTierLevel.GOLD) -> eligibilityProvider.isEligibleForSimpleBuy(
                    forceRefresh = true
                )
                    .map { eligible ->
                        if (eligible) {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_AND_ELIGIBLE)
                        } else {
                            SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED_BUT_NOT_ELIGIBLE)
                        }
                    }
                it.isRejectedFor(KycTierLevel.GOLD) -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.FAILED))
                it.isPendingFor(KycTierLevel.GOLD) -> Single.just(
                    SimpleBuyIntent.KycStateUpdated(KycState.IN_REVIEW)
                )
                else -> Single.just(SimpleBuyIntent.KycStateUpdated(KycState.PENDING))
            }
        }.onErrorReturn { SimpleBuyIntent.KycStateUpdated(KycState.PENDING) }
    }

    fun linkNewBank(fiatCurrency: FiatCurrency): Single<SimpleBuyIntent.BankLinkProcessStarted> {
        return bankService.linkBank(fiatCurrency).map { linkBankTransfer ->
            SimpleBuyIntent.BankLinkProcessStarted(linkBankTransfer)
        }
    }

    private fun KycTiers.isRejectedForAny(): Boolean =
        isRejectedFor(KycTierLevel.SILVER) ||
            isRejectedFor(KycTierLevel.GOLD)

    private fun KycTiers.isInReviewForAny(): Boolean =
        isUnderReviewFor(KycTierLevel.SILVER) ||
            isUnderReviewFor(KycTierLevel.GOLD)

    fun exchangeRate(asset: AssetInfo): Single<SimpleBuyIntent.ExchangePriceWithDeltaUpdated> =
        coincore.getExchangePriceWithDelta(asset)
            .map { exchangePriceWithDelta ->
                SimpleBuyIntent.ExchangePriceWithDeltaUpdated(exchangePriceWithDelta = exchangePriceWithDelta)
            }

    fun paymentMethods(fiatCurrency: FiatCurrency): Single<PaymentMethods> =
        tierService.tiers()
            .zipWith(
                custodialWalletManager.isSimplifiedDueDiligenceEligible().onErrorReturn { false }
                    .doOnSuccess {
                        if (it) {
                            analytics.logEventOnce(SDDAnalytics.SDD_ELIGIBLE)
                        }
                    }
            ).flatMap { (tier, sddEligible) ->
                Single.zip(
                    getAvailablePaymentMethodsTypesUseCase(
                        GetAvailablePaymentMethodsTypesUseCase.Request(
                            currency = fiatCurrency,
                            onlyEligible = tier.isInitialisedFor(KycTierLevel.GOLD),
                            fetchSddLimits = sddEligible && tier.isInInitialState()
                        )
                    ),
                    paymentMethodService.getLinkedPaymentMethods(
                        currency = fiatCurrency
                    )
                ) { available, linked ->
                    PaymentMethods(available, linked)
                }
            }

    // attributes are null in case of bank
    fun confirmOrder(
        orderId: String,
        paymentMethodId: String?,
        attributes: SimpleBuyConfirmationAttributes?,
        isBankPartner: Boolean?
    ): Single<BuySellOrder> {
        return custodialWalletManager.confirmOrder(
            orderId,
            attributes,
            paymentMethodId,
            isBankPartner
        )
    }

    fun pollForOrderStatus(orderId: String): Single<PollResult<BuySellOrder>> =
        PollService(custodialWalletManager.getBuyOrder(orderId)) {
            it.state == OrderState.FINISHED ||
                it.state == OrderState.FAILED ||
                it.state == OrderState.CANCELED
        }.start(INTERVAL, RETRIES_SHORT)

    fun pollForAuthorisationUrl(orderId: String): Single<PollResult<BuySellOrder>> =
        PollService(
            custodialWalletManager.getBuyOrder(orderId)
        ) {
            it.attributes?.authorisationUrl != null
        }.start()

    fun pollForCardStatus(cardId: String): Single<CardIntent.CardUpdated> =
        PollService(
            cardService.getCardDetails(cardId)
        ) {
            it.status == CardStatus.BLOCKED ||
                it.status == CardStatus.EXPIRED ||
                it.status == CardStatus.ACTIVE
        }
            .start()
            .map {
                CardIntent.CardUpdated(it.value)
            }

    fun eligiblePaymentMethodsTypes(fiatCurrency: FiatCurrency): Single<List<EligiblePaymentMethodType>> =
        paymentMethodService.getEligiblePaymentMethodTypes(fiatCurrency = fiatCurrency)

    fun getLinkedBankInfo(paymentMethodId: String) =
        bankService.getLinkedBank(paymentMethodId)

    fun fetchOrder(orderId: String) = custodialWalletManager.getBuyOrder(orderId)

    fun addNewCard(
        cardData: CardData,
        fiatCurrency: FiatCurrency,
        billingAddress: BillingAddress
    ): Single<CardToBeActivated> =
        addCardWithPaymentTokens(cardData, fiatCurrency, billingAddress)

    private fun addCardWithPaymentTokens(
        cardData: CardData,
        fiatCurrency: FiatCurrency,
        billingAddress: BillingAddress
    ) = custodialWalletManager.getCardAcquirers().flatMap { cardAcquirers ->
        rxSingle {
            // The backend is expecting a map of account codes and payment tokens.
            // Given that custodialWalletManager.getCardAcquirers() returns a list of PaymentCardAcquirers,
            // we need to map the PaymentCardAcquirers into payment tokens (string).
            cardAcquirers.associateWith { acquirer -> getPaymentToken(acquirer, cardData, billingAddress) }
        }.flatMap { acquirerTokenMap ->
            val acquirerAccountCodeTokensMap = acquirerTokenMap.filterValues { token ->
                token.isNotEmpty()
            }
                .flatMap { (acquirer, paymentToken) ->
                    acquirer.cardAcquirerAccountCodes.map { accountCode ->
                        accountCode to paymentToken
                    }
                }
                .associate { (accountCode, paymentToken) ->
                    accountCode to paymentToken
                }
            cardService.addNewCard(fiatCurrency, billingAddress, acquirerAccountCodeTokensMap)
        }
    }

    private suspend fun getPaymentToken(
        acquirer: PaymentCardAcquirer,
        cardData: CardData,
        billingAddress: BillingAddress
    ) = cardProcessors[CardAcquirer.fromString(acquirer.cardAcquirerName)]?.createPaymentMethod(
        cardDetails = cardData.toCardDetails(),
        billingAddress = billingAddress.toCardBillingAddress(),
        apiKey = acquirer.apiKey
    )?.doOnFailure { Timber.e(it.throwable) }
        ?.getOrDefault(EMPTY_PAYMENT_TOKEN)
        ?: EMPTY_PAYMENT_TOKEN

    fun updateApprovalStatus(callbackPath: String) {
        bankLinkingPrefs.getBankLinkingState().fromPreferencesValue()?.let {
            bankLinkingPrefs.setBankLinkingState(
                it.copy(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_PENDING).toPreferencesValue()
            )
        } ?: run {
            bankLinkingPrefs.setBankLinkingState(
                BankAuthDeepLinkState(bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_PENDING).toPreferencesValue()
            )
        }

        val sanitisedUrl = callbackPath.removePrefix("nabu-gateway/")
        bankLinkingPrefs.setDynamicOneTimeTokenUrl(sanitisedUrl)
    }

    fun updateOneTimeTokenPath(callbackPath: String) {
        val sanitisedUrl = callbackPath.removePrefix("nabu-gateway/")
        bankLinkingPrefs.setDynamicOneTimeTokenUrl(sanitisedUrl)
    }

    fun updateExchangeRate(fiat: FiatCurrency, asset: AssetInfo): Single<ExchangeRate> {
        return exchangeRatesDataManager.exchangeRate(asset, fiat).firstOrError()
    }

    fun getGooglePayInfo(
        currency: FiatCurrency
    ): Single<SimpleBuyIntent.GooglePayInfoReceived> =
        cardService.getGooglePayTokenizationParameters(
            currency = currency.networkTicker
        ).map {
            SimpleBuyIntent.GooglePayInfoReceived(
                tokenizationData = Json.decodeFromString(StringMapSerializer, it.googlePayParameters),
                beneficiaryId = it.beneficiaryID,
                merchantBankCountryCode = it.merchantBankCountryCode,
                allowPrepaidCards = it.allowPrepaidCards ?: true,
                allowCreditCards = it.allowCreditCards ?: true
            )
        }

    fun loadLinkedCards(): Single<List<LinkedPaymentMethod.Card>> =
        paymentsRepository.getLinkedCards(
            CardStatus.PENDING,
            CardStatus.ACTIVE
        )

    fun getLastPaymentMethodId() =
        simpleBuyPrefs.getLastPaymentMethodId().ifEmpty { null }

    fun shouldShowAppRating() =
        simpleBuyPrefs.buysCompletedCount >= APP_RATING_MINIMUM_BUY_ORDERS

    fun checkNewCardRejectionRate(binNumber: String): Single<CardRejectionState> =
        cardRejectionCheckFF.enabled.flatMap { enabled ->
            if (enabled) {
                rxSingle { paymentsRepository.checkNewCardRejectionState(binNumber).getOrThrow() }
            } else {
                // we don't want to block the user if the FF is off
                Single.just(CardRejectionState.NotRejected)
            }
        }

    data class PaymentMethods(
        val available: List<AvailablePaymentMethodType>,
        val linked: List<LinkedPaymentMethod>
    )

    private fun CardData.toCardDetails() =
        CardDetails(
            number = number,
            expMonth = month,
            expYear = year,
            cvc = cvv,
            fullName = fullName
        )

    private fun BillingAddress.toCardBillingAddress() =
        CardBillingAddress(
            city = city,
            country = countryCode,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            postalCode = postCode,
            state = state
        )

    fun updateCountersForCompletedOrders() {
        simpleBuyPrefs.hasCompletedAtLeastOneBuy = true
        simpleBuyPrefs.buysCompletedCount += 1
        onboardingPrefs.isLandingCtaDismissed = true
    }

    fun getPrefillAndQuickFillAmounts(
        buyMaxAmount: FiatValue,
        assetCode: String,
        fiatCurrency: FiatCurrency
    ): Single<Pair<FiatValue, QuickFillButtonData?>> =
        quickFillButtonsFeatureFlag.enabled.map { enabled ->
            var prefilledAmount = FiatValue.zero(fiatCurrency)

            val quickFillButtonData = if (enabled) {
                val amountString = simpleBuyPrefs.getLastAmount("$assetCode-${fiatCurrency.networkTicker}")
                    .ifEmpty {
                        DEFAULT_MIN_PREFILL_AMOUNT
                    }
                val listOfAmounts = mutableListOf<FiatValue>()

                prefilledAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(amountString))

                val lowestPrefillAmount = roundToNearest(
                    2 * prefilledAmount.toFloat(),
                    ROUND_TO_NEAREST_10
                )

                if (checkIfUnderLimit(lowestPrefillAmount, buyMaxAmount)) {
                    listOfAmounts.add(FiatValue.fromMajor(fiatCurrency, BigDecimal(lowestPrefillAmount)))
                }

                val mediumPrefillAmount = roundToNearest(
                    2 * lowestPrefillAmount.toFloat(),
                    ROUND_TO_NEAREST_50
                )
                if (checkIfUnderLimit(mediumPrefillAmount, buyMaxAmount)) {
                    listOfAmounts.add(FiatValue.fromMajor(fiatCurrency, BigDecimal(mediumPrefillAmount)))
                }

                val largestPrefillAmount = roundToNearest(
                    2 * mediumPrefillAmount.toFloat(),
                    ROUND_TO_NEAREST_100
                )
                if (checkIfUnderLimit(largestPrefillAmount, buyMaxAmount)) {
                    listOfAmounts.add(FiatValue.fromMajor(fiatCurrency, BigDecimal(largestPrefillAmount)))
                }

                QuickFillButtonData(buyMaxAmount = buyMaxAmount, quickFillButtons = listOfAmounts)
            } else {
                null
            }

            return@map Pair(prefilledAmount, quickFillButtonData)
        }

    fun getListOfStates(countryCode: String): Single<List<Region.State>> =
        rxSingleOutcome(Schedulers.io().asCoroutineDispatcher()) {
            eligibilityService.getStatesList(countryCode, GetRegionScope.None)
        }

    fun saveOrderAmountAndPaymentMethodId(pair: String, amount: String, paymentId: String) {
        simpleBuyPrefs.setLastAmount(
            pair = pair,
            amount = amount
        )
        simpleBuyPrefs.setLastPaymentMethodId(paymentMethodId = paymentId)
    }

    private fun roundToNearest(lastAmount: Float, nearest: Int): Int {
        return nearest * (floor(lastAmount.toDouble() / nearest) + 1).toInt()
    }

    private fun checkIfUnderLimit(fiatAmountToButton: Int, maxAmount: Money): Boolean =
        fiatAmountToButton <= maxAmount.toFloat()

    companion object {
        const val PENDING = "pending"

        private const val INTERVAL: Long = 5
        private const val RETRIES_SHORT = 6
        private const val RETRIES_DEFAULT = 12
        private const val EMPTY_PAYMENT_TOKEN: PaymentToken = ""

        private const val ROUND_TO_NEAREST_10 = 10
        private const val ROUND_TO_NEAREST_50 = 50
        private const val ROUND_TO_NEAREST_100 = 100
        private const val DEFAULT_MIN_PREFILL_AMOUNT = "50"
    }
}
