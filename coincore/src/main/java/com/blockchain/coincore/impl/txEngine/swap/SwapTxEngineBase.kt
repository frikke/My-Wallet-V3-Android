package com.blockchain.coincore.impl.txEngine.swap

import androidx.annotation.VisibleForTesting
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferDirection
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
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
import java.math.BigDecimal
import java.math.RoundingMode

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val RECEIVE_AMOUNT = "RECEIVE_AMOUNT"
const val OUTGOING_FEE = "OUTGOING_FEE"

abstract class SwapTxEngineBase(
    quotesEngine: TransferQuotesEngine,
    userIdentity: UserIdentity,
    private val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager
) : QuotedEngine(quotesEngine, userIdentity, walletManager, limitsDataManager, Product.TRADE) {

    private lateinit var minApiLimit: Money

    val target: CryptoAccount
        get() = txTarget as CryptoAccount

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.pricedQuote.map {
            ExchangeRate.CryptoToCrypto(
                from = sourceAsset,
                to = target.asset,
                rate = it.price.toBigDecimal()
            )
        }

    override fun onLimitsForTierFetched(
        limits: TxLimits,
        pendingTx: PendingTx,
        pricedQuote: PricedQuote
    ): PendingTx {
        minApiLimit = limits.min.amount

        return pendingTx.copy(
            minLimit = minLimit(pricedQuote.price),
            maxLimit = when (val max = limits.max) {
                is TxLimit.Limited -> max.amount
                TxLimit.Unlimited -> null
            }
        )
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun validateAmount(pendingTx: PendingTx): Completable {
        return availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                if (pendingTx.maxLimit != null && pendingTx.minLimit != null) {
                    when {
                        pendingTx.amount < pendingTx.minLimit -> throw TxValidationFailure(
                            ValidationState.UNDER_MIN_LIMIT
                        )
                        pendingTx.amount > pendingTx.maxLimit -> validationFailureForTier()
                        else -> Completable.complete()
                    }
                } else {
                    throw TxValidationFailure(ValidationState.UNKNOWN_ERROR)
                }
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    private fun buildConfirmations(pendingTx: PendingTx, pricedQuote: PricedQuote): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.SwapExchange(
                    CryptoValue.fromMajor(sourceAsset, BigDecimal.ONE),
                    CryptoValue.fromMajor(target.asset, pricedQuote.price.toBigDecimal())
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
                            target.asset
                        )
                )
            )
        ).also {
            it.engineState.copyAndPut(RECEIVE_AMOUNT, pricedQuote.price.toBigDecimal())
            it.engineState.copyAndPut(OUTGOING_FEE, pricedQuote.transferQuote.networkFee)
        }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.pricedQuote
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

    private fun addOrReplaceConfirmations(pendingTx: PendingTx, pricedQuote: PricedQuote): PendingTx =
        pendingTx.copy(
            minLimit = minLimit(pricedQuote.price)
        ).apply {
            addOrReplaceOption(
                TxConfirmationValue.SwapExchange(
                    CryptoValue.fromMajor(sourceAsset, BigDecimal.ONE),
                    CryptoValue.fromMajor(target.asset, pricedQuote.price.toBigDecimal())
                )
            )
            addOrReplaceOption(
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
                            target.asset
                        )
                )
            )
        }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        return quotesEngine.pricedQuote.firstOrError().map { pricedQuote ->
            addOrReplaceConfirmations(pendingTx, pricedQuote)
        }
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

    private fun minAmountToPayNetworkFees(price: Money, networkFee: Money): Money =
        CryptoValue.fromMajor(
            sourceAsset,
            networkFee.toBigDecimal()
                .divide(price.toBigDecimal(), sourceAsset.precisionDp, RoundingMode.HALF_UP)
        )
}
