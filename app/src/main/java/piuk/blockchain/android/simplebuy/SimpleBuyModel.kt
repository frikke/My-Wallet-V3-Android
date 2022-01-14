package piuk.blockchain.android.simplebuy

import com.blockchain.api.paymentmethods.models.SimpleBuyConfirmationAttributes
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.banking.BankTransferAction
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.LinkedPaymentMethod
import com.blockchain.core.payments.model.BankPartner
import com.blockchain.core.payments.model.BankState
import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.ApprovalErrorStatus
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CardAttributes
import com.blockchain.nabu.datamanagers.CardPaymentState
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.RecurringBuyOrder
import com.blockchain.nabu.datamanagers.UndefinedPaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorCodes
import com.blockchain.network.PollResult
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.RatingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.cards.CardAcquirerCredentials
import piuk.blockchain.android.cards.partners.CardActivator
import piuk.blockchain.android.domain.usecases.GetEligibilityAndNextPaymentDateUseCase
import piuk.blockchain.android.domain.usecases.IsFirstTimeBuyerUseCase
import piuk.blockchain.android.domain.usecases.LinkAccess
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionErrorState
import piuk.blockchain.android.util.ActivityIndicator
import piuk.blockchain.android.util.trackProgress
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class SimpleBuyModel(
    prefs: CurrencyPrefs,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val ratingPrefs: RatingPrefs,
    initialState: SimpleBuyState,
    uiScheduler: Scheduler,
    private val serializer: SimpleBuyPrefsSerializer,
    private val cardActivator: CardActivator,
    private val interactor: SimpleBuyInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    private val isFirstTimeBuyerUseCase: IsFirstTimeBuyerUseCase,
    private val getEligibilityAndNextPaymentDateUseCase: GetEligibilityAndNextPaymentDateUseCase,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger,
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider,
    private val userIdentity: UserIdentity
) : MviModel<SimpleBuyState, SimpleBuyIntent>(
    initialState = serializer.fetch() ?: initialState.withSelectedFiatCurrency(
        prefs.tradingCurrency
    ),
    uiScheduler = uiScheduler,
    environmentConfig = environmentConfig,
    crashLogger = crashLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(previousState: SimpleBuyState, intent: SimpleBuyIntent): Disposable? =
        when (intent) {
            is SimpleBuyIntent.UpdatedBuyLimits -> validateAmount(
                balance = previousState.availableBalance,
                amount = previousState.amount,
                buyLimits = intent.limits,
                paymentMethodLimits = previousState.selectedPaymentMethodLimits
            ).subscribeBy(
                onSuccess = {
                    process(SimpleBuyIntent.UpdateErrorState(it))
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.ValidateAmount -> validateAmount(
                balance = previousState.availableBalance,
                amount = previousState.amount,
                buyLimits = previousState.buyOrderLimits,
                paymentMethodLimits = previousState.selectedPaymentMethodLimits
            ).subscribeBy(
                onSuccess = {
                    process(SimpleBuyIntent.UpdateErrorState(it))
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.UpdateExchangeRate -> interactor.updateExchangeRate(intent.fiatCurrency, intent.asset)
                .subscribeBy(
                    onSuccess = { process(SimpleBuyIntent.ExchangeRateUpdated(it)) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )
            is SimpleBuyIntent.FetchSupportedFiatCurrencies ->
                interactor.fetchSupportedFiatCurrencies()
                    .subscribeBy(
                        onSuccess = { process(it) },
                        onError = { process(SimpleBuyIntent.ErrorIntent()) }
                    )
            is SimpleBuyIntent.CancelOrder,
            is SimpleBuyIntent.CancelOrderAndResetAuthorisation -> (
                previousState.id?.let {
                    interactor.cancelOrder(it)
                } ?: Completable.complete()
                ).trackProgress(activityIndicator)
                .subscribeBy(
                    onComplete = { process(SimpleBuyIntent.OrderCanceled) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )
            is SimpleBuyIntent.CancelOrderIfAnyAndCreatePendingOne -> (
                previousState.id?.let {
                    interactor.cancelOrder(it)
                } ?: Completable.complete()
                ).thenSingle {
                processCreateOrder(
                    previousState.selectedCryptoAsset,
                    previousState.selectedPaymentMethod,
                    previousState.order,
                    previousState.recurringBuyFrequency
                )
            }.subscribeBy(
                onSuccess = {
                    process(it)
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )

            is SimpleBuyIntent.FetchKycState -> interactor.pollForKycState()
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { /*never fails. will return SimpleBuyIntent.KycStateUpdated(KycState.FAILED)*/ }
                )

            is SimpleBuyIntent.LinkBankTransferRequested -> interactor.linkNewBank(previousState.fiatCurrency)
                .trackProgress(activityIndicator)
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported)) }
                )
            is SimpleBuyIntent.TryToLinkABankTransfer -> {
                interactor.eligiblePaymentMethodsTypes(previousState.fiatCurrency).map {
                    it.any { paymentMethod -> paymentMethod.type == PaymentMethodType.BANK_TRANSFER }
                }.subscribeBy(
                    onSuccess = { isEligibleToLinkABank ->
                        if (isEligibleToLinkABank) {
                            process(SimpleBuyIntent.LinkBankTransferRequested)
                        } else {
                            process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported))
                        }
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent(ErrorState.LinkedBankNotSupported))
                    }
                )
            }

            is SimpleBuyIntent.FetchWithdrawLockTime -> {
                require(previousState.selectedPaymentMethod != null)
                interactor.fetchWithdrawLockTime(
                    previousState.selectedPaymentMethod.paymentMethodType,
                    previousState.fiatCurrency
                ).subscribeBy(
                    onSuccess = { process(it) },
                    onError = { }
                )
            }
            is SimpleBuyIntent.BuyButtonClicked -> interactor.checkTierLevel()
                .subscribeBy(
                    onSuccess = { process(it) },
                    onError = { process(SimpleBuyIntent.ErrorIntent()) }
                )

            is SimpleBuyIntent.FetchPaymentDetails ->
                processGetPaymentMethod(
                    fiatCurrency = intent.fiatCurrency,
                    preselectedId = intent.selectedPaymentMethodId,
                    previousSelectedId = previousState.selectedPaymentMethod?.id
                )
            is SimpleBuyIntent.FetchSuggestedPaymentMethod ->
                processGetPaymentMethod(
                    fiatCurrency = intent.fiatCurrency,
                    preselectedId = intent.selectedPaymentMethodId,
                    previousSelectedId = previousState.selectedPaymentMethod?.id
                )

            is SimpleBuyIntent.PaymentMethodChangeRequested -> {
                validateAmountAndUpdatePaymentMethod(intent.paymentMethod, previousState)
            }
            is SimpleBuyIntent.PaymentMethodsUpdated -> {
                check(previousState.selectedCryptoAsset != null)
                intent.selectedPaymentMethod?.paymentMethodType?.let {
                    onPaymentMethodsUpdated(
                        previousState.selectedCryptoAsset,
                        previousState.fiatCurrency,
                        it
                    )
                }
            }
            is SimpleBuyIntent.MakePayment ->
                interactor.fetchOrder(intent.orderId)
                    .subscribeBy(
                        onError = {
                            process(SimpleBuyIntent.ErrorIntent())
                        },
                        onSuccess = {
                            if (it.attributes != null) {
                                handleOrderAttrs(it)
                            } else {
                                pollForOrderStatus()
                            }
                        }
                    )
            is SimpleBuyIntent.GetAuthorisationUrl ->
                interactor.pollForAuthorisationUrl(intent.orderId)
                    .subscribeBy(
                        onSuccess = {
                            when (it) {
                                is PollResult.FinalResult -> {
                                    it.value.attributes?.authorisationUrl?.let { url ->
                                        handleBankAuthorisationPayment(it.value.paymentMethodId, url)
                                    }
                                }
                                is PollResult.TimeOut -> process(
                                    SimpleBuyIntent.ErrorIntent(
                                        ErrorState.BankLinkingTimeout
                                    )
                                )
                                is PollResult.Cancel -> process(SimpleBuyIntent.ErrorIntent())
                            }
                        },
                        onError = {
                            process(SimpleBuyIntent.ErrorIntent())
                        }
                    )
            is SimpleBuyIntent.UpdatePaymentMethodsAndAddTheFirstEligible -> fetchEligiblePaymentMethods(
                intent.fiatCurrency
            ).trackProgress(activityIndicator).subscribeBy(
                onSuccess = {
                    process(
                        updateSelectedAndAvailablePaymentMethodMethodsIntent(
                            previousSelectedId = previousState.selectedPaymentMethod?.id,
                            availablePaymentMethods = it,
                            preselectedId = null
                        )
                    )

                    it.firstOrNull { paymentMethod -> paymentMethod.isEligible }
                        ?.let { paymentMethod ->
                            process(SimpleBuyIntent.AddNewPaymentMethodRequested(paymentMethod))
                        }
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.ConfirmOrder -> processConfirmOrder(
                previousState.id,
                previousState.selectedPaymentMethod
            )
            is SimpleBuyIntent.FinishedFirstBuy -> null
            is SimpleBuyIntent.CheckOrderStatus -> interactor.pollForOrderStatus(
                previousState.id ?: throw IllegalStateException("Order Id not available")
            ).subscribeBy(
                onSuccess = { buySellOrder ->
                    processOrderStatus(buySellOrder)
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            is SimpleBuyIntent.PaymentSucceeded -> {
                interactor.checkTierLevel().map { it.kycState != KycState.VERIFIED_AND_ELIGIBLE }.subscribeBy(
                    onSuccess = {
                        if (it) process(SimpleBuyIntent.UnlockHigherLimits)
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent())
                    }
                )
            }
            is SimpleBuyIntent.AppRatingShown -> {
                ratingPrefs.hasSeenRatingDialog = true
                null
            }

            is SimpleBuyIntent.RecurringBuySelectedFirstTimeFlow ->
                createRecurringBuy(
                    previousState.selectedCryptoAsset,
                    previousState.order,
                    previousState.selectedPaymentMethod,
                    intent.recurringBuyFrequency
                ).subscribeBy(
                    onSuccess = {
                        process(SimpleBuyIntent.RecurringBuyCreatedFirstTimeFlow)
                    },
                    onError = {
                        process(SimpleBuyIntent.ErrorIntent())
                    }
                )
            is SimpleBuyIntent.AmountUpdated -> validateAmount(
                balance = previousState.availableBalance,
                amount = intent.amount,
                buyLimits = previousState.buyOrderLimits,
                paymentMethodLimits = previousState.selectedPaymentMethodLimits
            ).subscribeBy(
                onSuccess = {
                    process(SimpleBuyIntent.UpdateErrorState(it))
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )
            else -> null
        }

    private fun processOrderStatus(buySellOrder: BuySellOrder) {
        when {
            buySellOrder.state == OrderState.FINISHED -> {
                updatePersistingCountersForCompletedOrders()
                process(SimpleBuyIntent.PaymentSucceeded)
            }
            buySellOrder.state.isPending() -> {
                process(SimpleBuyIntent.PaymentPending)
            }
            else -> {
                when (val cardAttributes = buySellOrder.attributes?.cardAttributes ?: CardAttributes.Empty) {
                    is CardAttributes.EveryPay -> {
                        handleCardPaymentState(cardAttributes.paymentState)
                    }
                    is CardAttributes.Provider -> {
                        handleCardPaymentState(cardAttributes.paymentState)
                    }
                    is CardAttributes.Empty -> {
                        // Usual path for any non-card payment
                        handleApprovalErrorState(buySellOrder.approvalErrorStatus)
                    }
                }
            }
        }
    }

    private fun onPaymentMethodsUpdated(
        asset: AssetInfo,
        fiatCurrency: FiatCurrency,
        selectedPaymentMethodType: PaymentMethodType
    ): Disposable {
        return interactor.fetchBuyLimits(
            fiat = fiatCurrency,
            asset = asset,
            paymentMethodType = selectedPaymentMethodType
        )
            .trackProgress(activityIndicator)
            .subscribeBy(
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                },
                onSuccess = { limits ->
                    process(
                        SimpleBuyIntent.UpdatedBuyLimits(
                            limits
                        )
                    )
                }
            )
    }

    private fun validateAmountAndUpdatePaymentMethod(
        paymentMethod: PaymentMethod,
        state: SimpleBuyState
    ): Disposable {
        val balance = paymentMethod.availableBalance
        require(state.selectedCryptoAsset != null)
        return interactor.fetchBuyLimits(
            fiat = state.fiatCurrency,
            asset = state.selectedCryptoAsset,
            paymentMethodType = paymentMethod.type
        ).trackProgress(activityIndicator).flatMap { limits ->
            validateAmount(
                amount = state.amount,
                balance = balance,
                buyLimits = limits,
                paymentMethodLimits = TxLimits.fromAmounts(paymentMethod.limits.min, paymentMethod.limits.max)
            ).map { errorState ->
                errorState to limits
            }
        }.subscribeBy(
            onSuccess = { (errorState, limits) ->
                process(SimpleBuyIntent.TxLimitsUpdated(limits))

                if (paymentMethod.isEligible && paymentMethod is UndefinedPaymentMethod) {
                    process(SimpleBuyIntent.AddNewPaymentMethodRequested(paymentMethod))
                } else {
                    process(SimpleBuyIntent.SelectedPaymentMethodUpdate(paymentMethod))
                    process(SimpleBuyIntent.UpdateErrorState(errorState))
                }
            },
            onError = {
                process(SimpleBuyIntent.ErrorIntent())
            }
        )
    }

    private fun validateAmount(
        amount: Money,
        balance: Money?,
        buyLimits: TxLimits,
        paymentMethodLimits: TxLimits
    ): Single<TransactionErrorState> =
        Single.defer {
            when {
                balance != null && balance < amount -> Single.just(TransactionErrorState.INSUFFICIENT_FUNDS)
                buyLimits.isMinViolatedByAmount(amount) -> Single.just(TransactionErrorState.BELOW_MIN_LIMIT)
                buyLimits.isMaxViolatedByAmount(amount) -> {
                    userIdentity.isVerifiedFor(Feature.TierLevel(Tier.GOLD)).onErrorReturnItem(false).map { gold ->
                        if (gold)
                            TransactionErrorState.OVER_GOLD_TIER_LIMIT
                        else
                            TransactionErrorState.OVER_SILVER_TIER_LIMIT
                    }
                }
                paymentMethodLimits.isMaxViolatedByAmount(amount) -> Single.just(
                    TransactionErrorState.ABOVE_MAX_PAYMENT_METHOD_LIMIT
                )

                paymentMethodLimits.isMinViolatedByAmount(amount) -> Single.just(
                    TransactionErrorState.BELOW_MIN_PAYMENT_METHOD_LIMIT
                )
                else -> Single.just(TransactionErrorState.NONE)
            }
        }

    private fun handleApprovalErrorState(approvalErrorStatus: ApprovalErrorStatus) {
        when (approvalErrorStatus) {
            ApprovalErrorStatus.FAILED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankFailed)
            )
            ApprovalErrorStatus.REJECTED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankRejected)
            )
            ApprovalErrorStatus.DECLINED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankDeclined)
            )
            ApprovalErrorStatus.EXPIRED -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedBankExpired)
            )
            ApprovalErrorStatus.UNKNOWN -> process(
                SimpleBuyIntent.ErrorIntent(ErrorState.ApprovedGenericError)
            )
            ApprovalErrorStatus.NONE -> {
                process(SimpleBuyIntent.ErrorIntent())
            }
        }.exhaustive
    }

    private fun handleCardPaymentState(paymentState: CardPaymentState) {
        // TODO: currently we continue polling as the OrderState will reflect what we need to do.
        // Discuss with product and design to handle the different states for card payments. Same for iOS.
        when (paymentState) {
            CardPaymentState.WAITING_FOR_3DS -> {
                // This is handled in handleCardPayment for now
            }
            CardPaymentState.INITIAL,
            CardPaymentState.CONFIRMED_3DS,
            CardPaymentState.SETTLED,
            CardPaymentState.VOIDED,
            CardPaymentState.ABANDONED,
            CardPaymentState.FAILED -> {
                // Continue polling
            }
        }
    }

    private fun processGetPaymentMethod(
        fiatCurrency: FiatCurrency,
        preselectedId: String?,
        previousSelectedId: String?
    ) =
        fetchEligiblePaymentMethods(fiatCurrency)
            .flatMap { paymentMethods ->
                getEligibilityAndNextPaymentDateUseCase(Unit)
                    .map { paymentMethods to it }
                    .onErrorReturn { paymentMethods to emptyList() }
            }
            .subscribeBy(
                onSuccess = { (availablePaymentMethods, eligibilityNextPaymentList) ->
                    process(SimpleBuyIntent.RecurringBuyEligibilityUpdated(eligibilityNextPaymentList))

                    process(
                        updateSelectedAndAvailablePaymentMethodMethodsIntent(
                            preselectedId = preselectedId,
                            previousSelectedId = previousSelectedId,
                            availablePaymentMethods = availablePaymentMethods
                        )
                    )
                },
                onError = {
                    process(SimpleBuyIntent.ErrorIntent())
                }
            )

    private fun fetchEligiblePaymentMethods(fiatCurrency: FiatCurrency) = interactor.paymentMethods(
        fiatCurrency
    ).map {
        it.toPaymentMethods(fiatCurrency)
    }

    private fun updateSelectedAndAvailablePaymentMethodMethodsIntent(
        preselectedId: String?,
        previousSelectedId: String?,
        availablePaymentMethods: List<PaymentMethod>
    ): SimpleBuyIntent.PaymentMethodsUpdated {
        val paymentOptions = PaymentOptions(
            availablePaymentMethods = availablePaymentMethods,
            canAddCard = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedCard>()
                .firstOrNull()?.isEligible ?: false,
            canLinkFunds = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedBankAccount>()
                .firstOrNull()?.isEligible ?: false,
            canLinkBank = availablePaymentMethods.filterIsInstance<PaymentMethod.UndefinedBankTransfer>()
                .firstOrNull()?.isEligible ?: false
        )

        val selectedPaymentMethodId = selectedMethodId(
            preselectedId = preselectedId,
            previousSelectedId = previousSelectedId,
            availablePaymentMethods = availablePaymentMethods
        )
        val selectedPaymentMethod = availablePaymentMethods.firstOrNull {
            it.id == selectedPaymentMethodId
        }

        return SimpleBuyIntent.PaymentMethodsUpdated(
            paymentOptions = paymentOptions,
            selectedPaymentMethod = selectedPaymentMethod?.let {
                SelectedPaymentMethod(
                    selectedPaymentMethod.id,
                    (selectedPaymentMethod as? PaymentMethod.Card)?.partner,
                    selectedPaymentMethod.detailedLabel(),
                    selectedPaymentMethod.type,
                    selectedPaymentMethod.isEligible
                )
            }
        )
    }

    // If no preselected Id and no payment method already set, we want the first eligible,
    // if none present, check if available is only 1 and preselect it. Otherwise, don't preselect anything
    private fun selectedMethodId(
        preselectedId: String?,
        previousSelectedId: String?,
        availablePaymentMethods: List<PaymentMethod>
    ): String? =
        preselectedId?.let { availablePaymentMethods.firstOrNull { it.id == preselectedId }?.id }
            ?: previousSelectedId?.let { availablePaymentMethods.firstOrNull { it.id == previousSelectedId }?.id }
            ?: let {
                val paymentMethodsThatCanBePreselected =
                    availablePaymentMethods.filter { it !is PaymentMethod.UndefinedBankAccount }
                paymentMethodsThatCanBePreselected.firstOrNull { it.isEligible && it.canBeUsedForPaying() }?.id
                    ?: paymentMethodsThatCanBePreselected.firstOrNull { it.isEligible }?.id
                    ?: paymentMethodsThatCanBePreselected.firstOrNull()?.id
            }

    private fun handleOrderAttrs(order: BuySellOrder) {
        when {
            order.attributes?.isCardPayment == true -> handleCardPayment(order)
            !order.source.currency.isOpenBankingCurrency() -> process(SimpleBuyIntent.CheckOrderStatus)
            order.attributes?.authorisationUrl != null -> handleBankAuthorisationPayment(
                paymentMethodId = order.paymentMethodId,
                authorisationUrl = order.attributes?.authorisationUrl!!
            )
            else -> process(SimpleBuyIntent.GetAuthorisationUrl(order.id))
        }
    }

    private fun Currency.isOpenBankingCurrency() =
        this.networkTicker == "EUR" || this.networkTicker == "GBP"

    private fun processCreateOrder(
        selectedCryptoAsset: AssetInfo?,
        selectedPaymentMethod: SelectedPaymentMethod?,
        order: SimpleBuyOrder,
        recurringBuyFrequency: RecurringBuyFrequency
    ): Single<SimpleBuyIntent.OrderCreated> {
        return isFirstTimeBuyer(recurringBuyFrequency)
            .flatMap { isFirstTimeBuyer ->
                createOrder(
                    selectedCryptoAsset,
                    selectedPaymentMethod,
                    order,
                    recurringBuyFrequency.takeIf { it != RecurringBuyFrequency.ONE_TIME }
                )
                    .map { isFirstTimeBuyer to it }
            }.map { (isFirstTimeBuyer, buySellOrder) ->
                if (isFirstTimeBuyer && recurringBuyFrequency == RecurringBuyFrequency.ONE_TIME) {
                    simpleBuyPrefs.isFirstTimeBuyer = false
                    process(SimpleBuyIntent.FinishedFirstBuy)
                }
                buySellOrder
            }
    }

    private fun isFirstTimeBuyer(recurringBuyFrequency: RecurringBuyFrequency): Single<Boolean> {
        return if (simpleBuyPrefs.isFirstTimeBuyer && recurringBuyFrequency == RecurringBuyFrequency.ONE_TIME) {
            isFirstTimeBuyerUseCase(Unit)
                .onErrorReturn { false }
        } else {
            Single.just(false)
        }
    }

    private fun createRecurringBuy(
        selectedCryptoAsset: AssetInfo?,
        order: SimpleBuyOrder,
        selectedPaymentMethod: SelectedPaymentMethod?,
        recurringBuyFrequency: RecurringBuyFrequency
    ): Single<RecurringBuyOrder> =
        interactor.createRecurringBuyOrder(
            selectedCryptoAsset,
            order,
            selectedPaymentMethod,
            recurringBuyFrequency
        )
            .onErrorReturn {
                RecurringBuyOrder(RecurringBuyState.INACTIVE)
            }

    private fun createOrder(
        selectedCryptoAsset: AssetInfo?,
        selectedPaymentMethod: SelectedPaymentMethod?,
        order: SimpleBuyOrder,
        recurringBuyFrequency: RecurringBuyFrequency?
    ): Single<SimpleBuyIntent.OrderCreated> {
        require(selectedCryptoAsset != null) { "Missing Cryptocurrency" }
        require(order.amount != null) { "Missing amount" }
        require(selectedPaymentMethod != null) { "Missing selectedPaymentMethod" }
        return interactor.createOrder(
            cryptoAsset = selectedCryptoAsset,
            amount = order.amount,
            paymentMethodId = selectedPaymentMethod.concreteId(),
            paymentMethod = selectedPaymentMethod.paymentMethodType,
            isPending = true,
            recurringBuyFrequency = recurringBuyFrequency
        )
    }

    private fun confirmOrder(
        id: String?,
        selectedPaymentMethod: SelectedPaymentMethod?
    ): Single<BuySellOrder> {
        require(id != null) { "Order Id not available" }
        require(selectedPaymentMethod != null) { "selectedPaymentMethod missing" }
        return interactor.confirmOrder(
            orderId = id,
            paymentMethodId = selectedPaymentMethod.takeIf { it.isBank() }?.concreteId(),
            attributes = if (selectedPaymentMethod.isBank()) {
                // redirectURL is for card providers only.
                SimpleBuyConfirmationAttributes(
                    callback = bankPartnerCallbackProvider.callback(BankPartner.YAPILY, BankTransferAction.PAY),
                    redirectURL = null
                )
            } else {
                cardActivator.paymentAttributes()
            },
            isBankPartner = selectedPaymentMethod.isBank()
        )
    }

    private fun processConfirmOrder(
        id: String?,
        selectedPaymentMethod: SelectedPaymentMethod?
    ): Disposable {
        return confirmOrder(id, selectedPaymentMethod).map { it }
            .subscribeBy(
                onSuccess = { buySellOrder ->
                    val orderCreatedSuccessfully = buySellOrder!!.state == OrderState.FINISHED
                    if (orderCreatedSuccessfully) {
                        updatePersistingCountersForCompletedOrders()
                    }
                    process(
                        SimpleBuyIntent.OrderConfirmed(
                            buySellOrder, shouldShowAppRating(orderCreatedSuccessfully)
                        )
                    )
                },
                onError = {
                    processOrderErrors(it)
                }
            )
    }

    private fun processOrderErrors(it: Throwable) {
        if (it is NabuApiException) {
            when (it.getErrorCode()) {
                NabuErrorCodes.DailyLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.DailyLimitExceeded)
                )
                NabuErrorCodes.WeeklyLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.WeeklyLimitExceeded)
                )
                NabuErrorCodes.AnnualLimitExceeded -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.YearlyLimitExceeded)
                )
                NabuErrorCodes.PendingOrdersLimitReached -> process(
                    SimpleBuyIntent.ErrorIntent(ErrorState.ExistingPendingOrder)
                )
                else -> process(SimpleBuyIntent.ErrorIntent())
            }
        } else {
            process(SimpleBuyIntent.ErrorIntent())
        }
    }

    private fun updatePersistingCountersForCompletedOrders() {
        ratingPrefs.preRatingActionCompletedTimes = ratingPrefs.preRatingActionCompletedTimes + 1
        simpleBuyPrefs.hasCompletedAtLeastOneBuy = true
    }

    private fun shouldShowAppRating(orderCreatedSuccessFully: Boolean): Boolean =
        ratingPrefs.preRatingActionCompletedTimes >= COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING &&
            !ratingPrefs.hasSeenRatingDialog && orderCreatedSuccessFully

    private fun pollForOrderStatus() {
        process(SimpleBuyIntent.CheckOrderStatus)
    }

    private fun handleCardPayment(order: BuySellOrder) {
        order.attributes?.cardAttributes?.let { paymentAttributes ->
            val intent = when (paymentAttributes) {
                is CardAttributes.Provider -> {
                    createIntentForCardProvider(paymentAttributes, order.state)
                }
                is CardAttributes.EveryPay -> {
                    if (paymentAttributes.paymentState == CardPaymentState.WAITING_FOR_3DS &&
                        order.state == OrderState.AWAITING_FUNDS
                    ) {
                        SimpleBuyIntent.Open3dsAuth(
                            CardAcquirerCredentials.Everypay(
                                paymentAttributes.paymentLink,
                                cardActivator.redirectUrl
                            )
                        )
                    } else {
                        SimpleBuyIntent.CheckOrderStatus
                    }
                }
                is CardAttributes.Empty -> SimpleBuyIntent.ErrorIntent() // todo handle case of partner not supported
            }
            process(intent)
        }
    }

    private fun createIntentForCardProvider(
        paymentAttributes: CardAttributes.Provider,
        orderState: OrderState
    ): SimpleBuyIntent {
        return if (orderState == OrderState.AWAITING_FUNDS) {
            when (CardAcquirer.fromString(paymentAttributes.cardAcquirerName)) {
                CardAcquirer.CHECKOUTDOTCOM -> SimpleBuyIntent.Open3dsAuth(
                    CardAcquirerCredentials.Checkout(
                        apiKey = paymentAttributes.publishableApiKey,
                        paymentLink = paymentAttributes.paymentLink,
                        exitLink = cardActivator.redirectUrl
                    )
                )
                CardAcquirer.EVERYPAY -> {
                    if (paymentAttributes.paymentState == CardPaymentState.WAITING_FOR_3DS) {
                        SimpleBuyIntent.Open3dsAuth(
                            CardAcquirerCredentials.Everypay(
                                paymentLink = paymentAttributes.paymentLink,
                                exitLink = cardActivator.redirectUrl
                            )
                        )
                    } else {
                        SimpleBuyIntent.CheckOrderStatus
                    }
                }
                CardAcquirer.STRIPE -> SimpleBuyIntent.Open3dsAuth(
                    CardAcquirerCredentials.Stripe(
                        apiKey = paymentAttributes.publishableApiKey,
                        clientSecret = paymentAttributes.clientSecret
                    )
                )
                CardAcquirer.UNKNOWN -> SimpleBuyIntent.ErrorIntent()
            }
        } else {
            SimpleBuyIntent.CheckOrderStatus
        }
    }

    private fun handleBankAuthorisationPayment(
        paymentMethodId: String,
        authorisationUrl: String
    ) {
        disposables += interactor.getLinkedBankInfo(paymentMethodId).subscribeBy(
            onSuccess = { linkedBank ->
                process(SimpleBuyIntent.AuthorisePaymentExternalUrl(authorisationUrl, linkedBank))
            },
            onError = {
                process(SimpleBuyIntent.ErrorIntent())
            }
        )
    }

    override fun onStateUpdate(s: SimpleBuyState) {
        serializer.update(s)
    }

    private fun SimpleBuyInteractor.PaymentMethods.toPaymentMethods(
        fiatCurrency: FiatCurrency
    ): List<PaymentMethod> {
        val availableMap = available.associateBy { it.type }
        val limitsMap = available.associate { it.type to it.limits }

        val availableForBuyLinkedMethods = linked
            .filter { availableMap[it.type]?.canBeUsedForPayment == true }
            .filter {
                when (it) {
                    is LinkedPaymentMethod.Card -> it.status == CardStatus.ACTIVE
                    is LinkedPaymentMethod.Funds -> it.currency == fiatCurrency
                    is LinkedPaymentMethod.Bank -> it.isBankTransferAccount && it.state == BankState.ACTIVE
                }
            }
            .filterNot { it.type == PaymentMethodType.FUNDS && it.currency != fiatCurrency }
            .mapNotNull {
                val limits = limitsMap[it.type] ?: return@mapNotNull null

                when (it) {
                    is LinkedPaymentMethod.Card -> PaymentMethod.Card(
                        cardId = it.cardId,
                        limits = limits,
                        label = it.label,
                        endDigits = it.endDigits,
                        partner = it.partner,
                        expireDate = it.expireDate,
                        cardType = it.cardType,
                        status = it.status,
                        isEligible = true
                    )
                    is LinkedPaymentMethod.Funds -> it.balance.takeIf { balance ->
                        balance > limits.min
                    }?.let { balance ->
                        val fundsLimits = limits.copy(max = Money.min(limits.max, balance))
                        PaymentMethod.Funds(
                            balance,
                            fiatCurrency,
                            fundsLimits,
                            true
                        )
                    }
                    is LinkedPaymentMethod.Bank -> PaymentMethod.Bank(
                        bankId = it.id,
                        limits = limits,
                        bankName = it.name,
                        accountEnding = it.accountEnding,
                        accountType = it.accountType,
                        iconUrl = it.iconUrl,
                        isEligible = true
                    )
                }
            }

        val canBeLinkedMethods = available
            .filter { it.linkAccess != LinkAccess.BLOCKED }
            .mapNotNull { method ->
                when (method.type) {
                    PaymentMethodType.PAYMENT_CARD ->
                        PaymentMethod.UndefinedCard(method.limits, method.canBeUsedForPayment)
                    PaymentMethodType.BANK_TRANSFER ->
                        PaymentMethod.UndefinedBankTransfer(method.limits, method.canBeUsedForPayment)
                    PaymentMethodType.BANK_ACCOUNT ->
                        if (method.canBeUsedForPayment && method.currency == fiatCurrency) {
                            PaymentMethod.UndefinedBankAccount(
                                method.currency,
                                method.limits,
                                method.canBeUsedForPayment
                            )
                        } else null
                    PaymentMethodType.FUNDS,
                    PaymentMethodType.UNKNOWN -> null
                }
            }

        val availablePaymentMethods = (availableForBuyLinkedMethods + canBeLinkedMethods)

        return availablePaymentMethods.sortedBy { paymentMethod -> paymentMethod.order }.toList()
    }

    companion object {
        const val COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING = 1
    }
}

private fun SimpleBuyState.withSelectedFiatCurrency(selectedFiatCurrency: Currency?): SimpleBuyState =
    (selectedFiatCurrency as? FiatCurrency)?.let {
        this.copy(
            fiatCurrency = selectedFiatCurrency,
            amount = Money.zero(selectedFiatCurrency) as FiatValue
        )
    } ?: this
