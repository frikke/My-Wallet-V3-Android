package com.blockchain.coincore.impl.txEngine.sell

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.core.TransactionsStore
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialSwapActivityStore
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith

class TradingSellTxEngine(
    private val tradingStore: TradingStore,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    quotesEngine: TransferQuotesEngine,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userIdentity: UserIdentity
) : SellTxEngineBase(walletManager, limitsDataManager, userIdentity, quotesEngine) {

    private val swapActivityStore: CustodialSwapActivityStore by scopedInject()
    private val transactionsStore: TransactionsStore by scopedInject()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(tradingStore, transactionsStore, swapActivityStore)

    override fun ensureSourceBalanceFreshness() {
        tradingStore.markAsStale()
    }

    override val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    override val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map {
            it.total
        }

    override fun assertInputsValid() {
        check(sourceAccount is CustodialTradingAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.getPricedQuote().firstOrError()
            .zipWith(availableBalance)
            .flatMap { (quote, balance) ->
                Single.just(
                    PendingTx(
                        amount = Money.zero(sourceAsset),
                        totalBalance = balance,
                        availableBalance = balance,
                        feeAmount = Money.zero(sourceAsset),
                        feeForFullAvailable = Money.zero(sourceAsset),
                        selectedFiat = target.currency,
                        feeSelection = FeeSelection()
                    )
                ).flatMap {
                    updateLimits(target.currency, it, quote)
                }
            }.handlePendingOrdersError(
                PendingTx(
                    amount = Money.zero(sourceAsset),
                    totalBalance = Money.zero(sourceAsset),
                    availableBalance = Money.zero(sourceAsset),
                    feeForFullAvailable = Money.zero(sourceAsset),
                    feeAmount = Money.zero(sourceAsset),
                    selectedFiat = target.currency,
                    feeSelection = FeeSelection()
                )
            )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return availableBalance
            .map { it as CryptoValue }
            .map { available ->
                pendingTx.copy(
                    amount = amount,
                    availableBalance = available,
                    totalBalance = available
                )
            }
            .updateQuotePrice()
            .clearConfirmations()
    }

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    override fun feeInSourceCurrency(pendingTx: PendingTx): Money = pendingTx.feeAmount

    override val requireSecondPassword: Boolean
        get() = false

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createSellOrder(pendingTx).map {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }
}
