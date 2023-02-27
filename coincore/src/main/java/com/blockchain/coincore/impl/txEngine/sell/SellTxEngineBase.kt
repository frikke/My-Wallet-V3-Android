package com.blockchain.coincore.impl.txEngine.sell

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.txEngine.MissingLimitsException
import com.blockchain.coincore.impl.txEngine.PricedQuote
import com.blockchain.coincore.impl.txEngine.QuotedEngine
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

const val LATEST_QUOTE_ID = "LATEST_QUOTE_ID"
private val PendingTx.quoteId: String?
    get() = (this.engineState[LATEST_QUOTE_ID] as? String)

abstract class SellTxEngineBase(
    private val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    userIdentity: UserIdentity,
    quotesEngine: TransferQuotesEngine
) : QuotedEngine(quotesEngine, userIdentity, walletManager, limitsDataManager, Product.SELL) {

    val target: FiatAccount
        get() = txTarget as FiatAccount

    override fun onLimitsForTierFetched(
        limits: TxLimits,
        pendingTx: PendingTx,
        quotePrice: QuotePrice,
    ): PendingTx = pendingTx.copy(
        limits = limits
    )

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
                    Completable.error(
                        MissingLimitsException(action = AssetAction.Sell, source = sourceAccount, target = txTarget)
                    )
                }
            } else {
                Completable.error(
                    TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                )
            }
        }
    }

    // The fee for on chain transaction for erc20 tokens is 0 for the corresponding erc20 token.
    // The fee for those transactions is paid in ETH and the tx validation happens in the Erc20OnChainEngine
    abstract fun feeInSourceCurrency(pendingTx: PendingTx): Money

    private fun buildNewFee(pendingTx: PendingTx): TxConfirmationValue {
        return TxConfirmationValue.CompoundNetworkFee(
            if (pendingTx.feeAmount.isZero) {
                null
            } else
                FeeInfo(
                    pendingTx.feeAmount,
                    pendingTx.feeAmount.toUserFiat(exchangeRates),
                    sourceAsset
                )
        )
    }

    private fun buildConfirmation(
        pendingTx: PendingTx,
        latestQuoteExchangeRate: ExchangeRate,
        pricedQuote: PricedQuote
    ): PendingTx =
        pendingTx.copy(
            txConfirmations = listOfNotNull(
                TxConfirmationValue.QuoteCountDown(
                    pricedQuote
                ),
                TxConfirmationValue.ExchangePriceConfirmation(
                    money = pricedQuote.transferQuote.price,
                    asset = sourceAsset,
                    isNewQuote = false
                ),
                TxConfirmationValue.To(
                    txTarget,
                    AssetAction.Sell
                ),
                TxConfirmationValue.Sale(
                    amount = pendingTx.amount,
                    exchange = latestQuoteExchangeRate.convert(pendingTx.amount),
                    isNewQuote = false
                ),
                buildNewFee(pendingTx),
                // In case of  PK ERC20s token, Total is displayed without the fee. As the fee is in ETH
                // we need a different design to present here both the selling amount and the applied fee
                TxConfirmationValue.Total(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        feeInSourceCurrency(pendingTx)
                    ),
                    exchange = latestQuoteExchangeRate.convert(
                        pendingTx.amount.plus(feeInSourceCurrency(pendingTx))
                    ),
                    isNewQuote = false
                ),
            )
        )

    override fun targetExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.getPriceExchangeRate()

    override fun confirmationExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.getQuoteExchangeRate()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        quotesEngine.getQuote()
            .firstOrError()
            .map { pricedQuote ->
                val latestQuoteExchangeRate = ExchangeRate(
                    from = sourceAsset,
                    to = target.currency,
                    rate = pricedQuote.transferQuote.price.toBigDecimal()
                )
                buildConfirmation(pendingTx, latestQuoteExchangeRate, pricedQuote)
            }

    private fun addOrRefreshConfirmations(
        pendingTx: PendingTx,
        pricedQuote: PricedQuote,
        latestQuoteExchangeRate: ExchangeRate,
        isNewQuote: Boolean,
    ): PendingTx {
        return pendingTx.addOrReplaceOption(
            TxConfirmationValue.QuoteCountDown(
                pricedQuote
            )
        ).addOrReplaceOption(
            TxConfirmationValue.ExchangePriceConfirmation(
                money = pricedQuote.transferQuote.price,
                asset = sourceAsset,
                isNewQuote = isNewQuote,
            )
        ).addOrReplaceOption(
            TxConfirmationValue.Sale(
                amount = pendingTx.amount,
                exchange = latestQuoteExchangeRate.convert(pendingTx.amount),
                isNewQuote = isNewQuote,
            )
        ).addOrReplaceOption(
            TxConfirmationValue.Total(
                totalWithFee = (pendingTx.amount as CryptoValue).plus(
                    feeInSourceCurrency(pendingTx)
                ),
                exchange = latestQuoteExchangeRate.convert(
                    pendingTx.amount.plus(feeInSourceCurrency(pendingTx))
                ),
                isNewQuote = isNewQuote
            )
        )
    }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> {
        val quote = quotesEngine.getLatestQuote()
        val latestQuoteExchangeRate = ExchangeRate(
            from = sourceAsset,
            to = target.currency,
            rate = quote.transferQuote.price.toBigDecimal()
        )
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

        return Single.just(addOrRefreshConfirmations(ptx, quote, latestQuoteExchangeRate, isNewQuote))
    }

    protected fun createSellOrder(pendingTx: PendingTx): Single<CustodialOrder> =
        sourceAccount.receiveAddress
            .onErrorReturn { NullAddress }
            .flatMap { refAddress ->
                walletManager.createCustodialOrder(
                    direction = direction,
                    quoteId = quotesEngine.getLatestQuote().transferQuote.id,
                    volume = pendingTx.amount,
                    refundAddress = if (direction.requiresRefundAddress()) refAddress.address else null
                ).doFinally {
                    disposeQuotesFetching(pendingTx)
                }
            }

    private fun TransferDirection.requiresRefundAddress() =
        this == TransferDirection.FROM_USERKEY

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun userExchangeRate(): Observable<ExchangeRate> =
        quotesEngine.getPriceExchangeRate()
}
