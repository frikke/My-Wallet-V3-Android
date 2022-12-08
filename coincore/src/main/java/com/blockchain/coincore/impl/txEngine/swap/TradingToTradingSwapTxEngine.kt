package com.blockchain.coincore.impl.txEngine.swap

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class TradingToTradingSwapTxEngine(
    private val tradingStore: TradingStore,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    swapTransactionsStore: SwapTransactionsStore,
    quotesEngine: TransferQuotesEngine,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userIdentity: UserIdentity,
) : SwapTxEngineBase(quotesEngine, userIdentity, walletManager, limitsDataManager, swapTransactionsStore) {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(tradingStore)

    override val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx.firstOrError().map {
            it.total
        }

    override fun assertInputsValid() {
        check(txTarget is CustodialTradingAccount)
        check(sourceAccount is CustodialTradingAccount)
        check((txTarget as CustodialTradingAccount).currency != sourceAsset)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.getPricedQuote().firstOrError()
            .flatMap { pricedQuote ->
                availableBalance.flatMap { balance ->
                    Single.just(
                        PendingTx(
                            amount = Money.zero(sourceAsset),
                            totalBalance = balance,
                            availableBalance = balance,
                            feeForFullAvailable = Money.zero(sourceAsset),
                            feeAmount = Money.zero(sourceAsset),
                            feeSelection = FeeSelection(),
                            selectedFiat = userFiat
                        )
                    ).flatMap {
                        updateLimits(userFiat, it, pricedQuote)
                    }
                }
            }.handlePendingOrdersError(
                PendingTx(
                    amount = Money.zero(sourceAsset),
                    totalBalance = Money.zero(sourceAsset),
                    availableBalance = Money.zero(sourceAsset),
                    feeForFullAvailable = Money.zero(sourceAsset),
                    feeAmount = Money.zero(sourceAsset),
                    feeSelection = FeeSelection(),
                    selectedFiat = userFiat
                )
            )

    override val direction: TransferDirection
        get() = TransferDirection.INTERNAL

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createOrder(pendingTx).map {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.map { balance ->
            balance as CryptoValue
        }.map { available ->
            pendingTx.copy(
                amount = amount,
                availableBalance = available,
                totalBalance = available
            )
        }.updateQuotePrice().clearConfirmations()

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long,
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        // This engine only supports FeeLevel.None, so
        return Single.just(pendingTx)
    }

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.None)
    }
}
