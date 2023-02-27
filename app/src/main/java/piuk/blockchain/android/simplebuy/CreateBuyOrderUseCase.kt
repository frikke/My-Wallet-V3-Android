package piuk.blockchain.android.simplebuy

import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.custodial.models.BuyOrderAndQuote
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.RecurringBuyFrequency
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderInput
import com.blockchain.nabu.datamanagers.OrderOutput
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
import com.blockchain.utils.thenSingle
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import timber.log.Timber

class CreateBuyOrderUseCase(
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val brokerageDataManager: BrokerageDataManager,
    private val custodialWalletManager: CustodialWalletManager,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    buyQuoteRefreshFF: FeatureFlag,
) {
    private var latestPendingOrder: BuyOrderAndQuote? = null
    private val stop = PublishSubject.create<Unit>()
    private val compositeDisposable = CompositeDisposable()
    private val subject: PublishSubject<Outcome<Throwable, BuyOrderAndQuote>> = PublishSubject.create()
    val buyOrderAndQuote: Observable<Outcome<Throwable, BuyOrderAndQuote>>
        get() = subject.doOnNext {
            it.getOrNull()?.let { buyOrderAndQuote ->
                Timber.i("New quote ${buyOrderAndQuote.buyOrder.id} --- ${buyOrderAndQuote.quote.secondsToExpire}")
                latestPendingOrder = buyOrderAndQuote
            }
        }

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    private val featureFlag = buyQuoteRefreshFF.enabled

    fun createOrderAndStartsQuotesFetching(
        oldStateId: String?,
        selectedCryptoAsset: AssetInfo?,
        selectedPaymentMethod: SelectedPaymentMethod?,
        order: SimpleBuyOrder,
        recurringBuyFrequency: RecurringBuyFrequency,
    ) {
        compositeDisposable.clear()

        val newOrder: (oldId: String?) -> Single<BuyOrderAndQuote> =
            { oldId ->
                processCreateOrder(
                    oldId,
                    selectedCryptoAsset,
                    selectedPaymentMethod,
                    order = order,
                    recurringBuyFrequency
                ).trackProgress(activityIndicator)
            }

        val orderToGetCancelled = latestPendingOrder?.let {
            it.buyOrder.id
        } ?: oldStateId

        val updateOrder =
            newOrder(orderToGetCancelled).zipWith(featureFlag).flatMapObservable { (buyOrderAndQuote, enabled) ->
                if (enabled) {
                    val quoteTimeToExpire = buyOrderAndQuote.quote.millisToExpire()
                    Observable.interval(
                        quoteTimeToExpire,
                        quoteTimeToExpire,
                        TimeUnit.MILLISECONDS
                    ).map {
                        latestPendingOrder ?: throw IllegalStateException("Missing quote for original order")
                    }.flatMapSingle { lastBuyAndQuoteOrder ->
                        newOrder(lastBuyAndQuoteOrder.buyOrder.id)
                    }.startWithItem(buyOrderAndQuote)
                } else {
                    Observable.just(buyOrderAndQuote)
                }
            }

        compositeDisposable += updateOrder.takeUntil(stop).subscribeBy(
            onError = { subject.onNext(Outcome.Failure(it)) },
            onNext = { subject.onNext(Outcome.Success(it)) }
        )
    }

    private fun fetchQuoteAndCreateOrder(
        cryptoAsset: AssetInfo,
        amount: Money,
        paymentMethodId: String? = null,
        paymentMethod: PaymentMethodType,
        recurringBuyFrequency: RecurringBuyFrequency?,
    ): Single<BuyOrderAndQuote> =
        brokerageDataManager.getBuyQuote(
            pair = CurrencyPair(amount.currency, cryptoAsset),
            amount = amount,
            paymentMethodType = getPaymentMethodType(paymentMethod),
            paymentMethodId = getPaymentMethodId(paymentMethodId, paymentMethod),
        ).flatMap { quote ->
            custodialWalletManager.createOrder(
                custodialWalletOrder = CustodialWalletOrder(
                    quoteId = quote.id,
                    pair = "${cryptoAsset.networkTicker}-${amount.currencyCode}",
                    action = Product.BUY.name,
                    input = OrderInput(
                        amount.currencyCode, amount.toBigInteger().toString()
                    ),
                    output = OrderOutput(
                        cryptoAsset.networkTicker, null
                    ),
                    paymentMethodId = getPaymentMethodId(paymentMethodId, paymentMethod),
                    paymentType = getPaymentMethodType(paymentMethod).name,
                    period = recurringBuyFrequency?.name
                ),
                stateAction = "pending"
            ).map {
                BuyOrderAndQuote(buyOrder = it, quote = quote)
            }
        }

    private fun processCreateOrder(
        oldId: String?,
        selectedCryptoAsset: AssetInfo?,
        selectedPaymentMethod: SelectedPaymentMethod?,
        order: SimpleBuyOrder,
        recurringBuyFrequency: RecurringBuyFrequency,
    ): Single<BuyOrderAndQuote> {
        return (
            oldId?.let {
                cancelOrderUseCase.invoke(it)
            } ?: Completable.complete()
            ).thenSingle {
            createOrder(
                selectedCryptoAsset = selectedCryptoAsset,
                selectedPaymentMethod = selectedPaymentMethod,
                order = order,
                recurringBuyFrequency = recurringBuyFrequency.takeIf { it != RecurringBuyFrequency.ONE_TIME },
            )
        }
    }

    private fun createOrder(
        selectedCryptoAsset: AssetInfo?,
        selectedPaymentMethod: SelectedPaymentMethod?,
        order: SimpleBuyOrder,
        recurringBuyFrequency: RecurringBuyFrequency?,
    ): Single<BuyOrderAndQuote> {
        require(selectedCryptoAsset != null) { "Missing Cryptocurrency" }
        require(order.amount != null) { "Missing amount" }
        require(selectedPaymentMethod != null) { "Missing selectedPaymentMethod" }
        return fetchQuoteAndCreateOrder(
            cryptoAsset = selectedCryptoAsset,
            amount = order.amount,
            paymentMethodId = selectedPaymentMethod.concreteId(),
            paymentMethod = selectedPaymentMethod.paymentMethodType,
            recurringBuyFrequency = recurringBuyFrequency,
        )
    }

    /**
     * This flag determines if we need to persist the order anymore in the use case,
     * so we can cancel later, before creating a new one
     */
    fun stopQuoteFetching(shouldResetOrder: Boolean) {
        stop.onNext(Unit)
        compositeDisposable.clear()
        if (shouldResetOrder) {
            latestPendingOrder = null
        }
    }

    private fun getPaymentMethodType(paymentMethod: PaymentMethodType) =
        // The API cannot handle GOOGLE_PAY as a payment method, so we're treating this as a card
        if (paymentMethod == PaymentMethodType.GOOGLE_PAY) PaymentMethodType.PAYMENT_CARD else paymentMethod

    private fun getPaymentMethodId(paymentMethodId: String? = null, paymentMethod: PaymentMethodType) =
        // The API cannot handle GOOGLE_PAY as a payment method, so we're sending a null paymentMethodId
        if (paymentMethod == PaymentMethodType.GOOGLE_PAY || paymentMethodId == PaymentMethod.GOOGLE_PAY_PAYMENT_ID)
            null
        else paymentMethodId
}
