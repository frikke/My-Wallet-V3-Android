package com.blockchain.coincore.impl.txEngine.swap

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.txEngine.PricedQuote
import com.blockchain.coincore.impl.txEngine.QuotedEngine
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.math.BigDecimal
import java.math.RoundingMode

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val RECEIVE_AMOUNT = "RECEIVE_AMOUNT"
const val OUTGOING_FEE = "OUTGOING_FEE"
const val LATEST_QUOTE_ID = "LATEST_QUOTE_ID"
private val PendingTx.quoteId: String?
    get() = (this.engineState[LATEST_QUOTE_ID] as? String)

abstract class SwapTxEngineBase(
    quotesEngine: TransferQuotesEngine,
    userIdentity: UserIdentity,
    private val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    private val swapTransactionsStore: SwapTransactionsStore,
) : QuotedEngine(quotesEngine, userIdentity, walletManager, limitsDataManager, Product.TRADE) {

    private lateinit var minApiLimit: Money

    val target: CryptoAccount
        get() = txTarget as CryptoAccount

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.getPricedQuote().map {
            ExchangeRate(
                from = sourceAsset,
                to = target.currency,
                rate = it.price.toBigDecimal()
            )
        }

    override fun onLimitsForTierFetched(
        limits: TxLimits,
        pendingTx: PendingTx,
        pricedQuote: PricedQuote,
    ): PendingTx {
        minApiLimit = limits.min.amount

        return pendingTx.copy(
            limits = limits.copy(
                min = TxLimit.Limited(minLimit(pricedQuote.price))
            )
        )
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmount(pendingTx: PendingTx): Completable {
        return availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                if (pendingTx.limits != null) {
                    when {
                        pendingTx.isMinLimitViolated() -> Completable.error(
                            TxValidationFailure(
                                ValidationState.UNDER_MIN_LIMIT
                            )
                        )
                        pendingTx.isMaxLimitViolated() -> validationFailureForTier()
                        else -> Completable.complete()
                    }
                } else {
                    Completable.error(TxValidationFailure(ValidationState.UNINITIALISED))
                }
            } else {
                Completable.error(TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS))
            }
        }
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun buildConfirmations(pendingTx: PendingTx, pricedQuote: PricedQuote): PendingTx {
        return pendingTx.copy(
            txConfirmations = listOfNotNull(
                TxConfirmationValue.SwapExchange(
                    unitCryptoCurrency = Money.fromMajor(sourceAsset, BigDecimal.ONE),
                    price = Money.fromMajor(target.currency, pricedQuote.price.toBigDecimal()),
                    isNewQuote = false
                ),
                TxConfirmationValue.CompoundNetworkFee(
                    if (pendingTx.feeAmount.isZero) {
                        null
                    } else
                        FeeInfo(
                            pendingTx.feeAmount,
                            pendingTx.feeAmount.toUserFiat(exchangeRates),
                            sourceAsset
                        ),
                    if (pricedQuote.transferQuote.networkFee.isZero) {
                        null
                    } else
                        FeeInfo(
                            pricedQuote.transferQuote.networkFee,
                            pricedQuote.transferQuote.networkFee.toUserFiat(exchangeRates),
                            target.currency
                        )
                ),
                TxConfirmationValue.QuoteCountDown(
                    pricedQuote = pricedQuote
                )
            ),
            engineState = pendingTx.engineState
                .copyAndPut(LATEST_QUOTE_ID, pricedQuote.transferQuote.id)
                .copyAndPut(RECEIVE_AMOUNT, pricedQuote.price.toBigDecimal())
                .copyAndPut(OUTGOING_FEE, pricedQuote.transferQuote.networkFee)
        )
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.getPricedQuote()
            .firstOrError()
            .map { pricedQuote ->
                buildConfirmations(pendingTx, pricedQuote)
            }

    private fun minLimit(price: Money): Money {
        val minAmountToPayFees = minAmountToPayNetworkFees(
            price,
            quotesEngine.getLatestQuote().transferQuote.networkFee
        )
        return minApiLimit.plus(minAmountToPayFees).withUserDpRounding(RoundingMode.CEILING)
    }

    private fun addOrReplaceConfirmations(
        pendingTx: PendingTx,
        pricedQuote: PricedQuote,
        isNewQuote: Boolean
    ): PendingTx {
        return pendingTx.copy(
            limits = pendingTx.limits?.copy(min = TxLimit.Limited(minLimit(pricedQuote.price)))
        ).addOrReplaceOption(
            TxConfirmationValue.SwapExchange(
                Money.fromMajor(sourceAsset, BigDecimal.ONE),
                Money.fromMajor(target.currency, pricedQuote.price.toBigDecimal()),
                isNewQuote
            )
        ).addOrReplaceOption(
            TxConfirmationValue.CompoundNetworkFee(
                if (pendingTx.feeAmount.isZero) {
                    null
                } else
                    FeeInfo(
                        pendingTx.feeAmount,
                        pendingTx.feeAmount.toUserFiat(exchangeRates),
                        sourceAsset
                    ),
                if (pricedQuote.transferQuote.networkFee.isZero) {
                    null
                } else
                    FeeInfo(
                        pricedQuote.transferQuote.networkFee,
                        pricedQuote.transferQuote.networkFee.toUserFiat(exchangeRates),
                        target.currency
                    )
            )
        ).addOrReplaceOption(
            TxConfirmationValue.QuoteCountDown(
                pricedQuote
            )
        )
    }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        val quote = quotesEngine.getLatestQuote()
        val isNewQuote = pendingTx.quoteId != quote.transferQuote.id
        val ptx = if (isNewQuote) {
            pendingTx.copy(
                engineState = pendingTx.engineState.copyAndPut(
                    LATEST_QUOTE_ID, quote.transferQuote.id
                )
            )
        } else {
            pendingTx.copy()
        }
        return Single.just(addOrReplaceConfirmations(ptx, quote, isNewQuote))
    }

    protected fun createOrder(pendingTx: PendingTx): Single<CustodialOrder> =
        target.receiveAddress.zipWith(sourceAccount.receiveAddress.onErrorReturn { NullAddress })
            .flatMap { (destinationAddr, refAddress) ->
                walletManager.createCustodialOrder(
                    direction = direction,
                    quoteId = quotesEngine.getLatestQuote().transferQuote.id,
                    volume = pendingTx.amount,
                    destinationAddress = if (direction.requiresDestinationAddress()) destinationAddr.address else null,
                    refundAddress = if (direction.requireRefundAddress()) refAddress.address else null
                )
            }.doFinally {
                disposeQuotesFetching(pendingTx)
            }

    private fun TransferDirection.requiresDestinationAddress() =
        this == TransferDirection.ON_CHAIN || this == TransferDirection.TO_USERKEY

    private fun TransferDirection.requireRefundAddress() =
        this == TransferDirection.ON_CHAIN || this == TransferDirection.FROM_USERKEY

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable {
        return Completable.fromCallable {
            swapTransactionsStore.invalidate()
        }
    }

    private fun minAmountToPayNetworkFees(price: Money, networkFee: Money): Money =
        Money.fromMajor(
            sourceAsset,
            networkFee.toBigDecimal()
                .divide(price.toBigDecimal(), sourceAsset.precisionDp, RoundingMode.HALF_UP)
        )
}
