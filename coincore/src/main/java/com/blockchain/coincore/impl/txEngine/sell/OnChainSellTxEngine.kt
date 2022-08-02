package com.blockchain.coincore.impl.txEngine.sell

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.erc20.isErc20
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class OnChainSellTxEngine(
    private val tradingStore: TradingStore,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val engine: OnChainTxEngineBase,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userIdentity: UserIdentity,
    quotesEngine: TransferQuotesEngine
) : SellTxEngineBase(
    walletManager, limitsDataManager, userIdentity, quotesEngine
) {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(tradingStore)

    override val direction: TransferDirection
        get() = TransferDirection.FROM_USERKEY

    override val availableBalance: Single<Money>
        get() = sourceAccount.balance.firstOrError().map {
            it.total
        }

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.getPricedQuote()
            .firstOrError()
            .doOnSuccess { pricedQuote ->
                engine.startFromQuote(pricedQuote)
            }.flatMap { quote ->
                engine.doInitialiseTx()
                    .flatMap {
                        updateLimits(target.currency, it, quote)
                    }
            }.map { px ->
                px.copy(
                    feeSelection = defaultFeeSelection(px),
                    selectedFiat = target.currency
                )
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

    private fun defaultFeeSelection(pendingTx: PendingTx): FeeSelection =
        when {
            pendingTx.feeSelection.availableLevels.contains(FeeLevel.Priority) -> {
                pendingTx.feeSelection.copy(
                    selectedLevel = FeeLevel.Priority,
                    availableLevels = setOf(FeeLevel.Priority)
                )
            }
            pendingTx.feeSelection.availableLevels.contains(FeeLevel.Regular) -> {
                pendingTx.feeSelection.copy(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = setOf(FeeLevel.Regular)
                )
            }
            else -> throw Exception("Not supported")
        }

    override fun feeInSourceCurrency(pendingTx: PendingTx): Money =
        if (sourceAsset.isErc20()) Money.zero(sourceAsset)
        else pendingTx.feeAmount

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        engine.doValidateAmount(pendingTx)
            .flatMap {
                if (
                    it.validationState == ValidationState.CAN_EXECUTE ||
                    it.validationState == ValidationState.INVALID_AMOUNT
                ) {
                    super.doValidateAmount(pendingTx)
                } else {
                    Single.just(it)
                }
            }.updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        engine.doValidateAll(pendingTx)
            .flatMap {
                if (
                    it.validationState == ValidationState.CAN_EXECUTE ||
                    it.validationState == ValidationState.INVALID_AMOUNT
                ) {
                    super.doValidateAll(pendingTx)
                } else {
                    Single.just(it)
                }
            }.updateTxValidity(pendingTx)

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        engine.doUpdateAmount(amount, pendingTx)
            .updateQuotePrice()
            .clearConfirmations()

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> = Single.just(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createSellOrder(pendingTx)
            .flatMap { order ->
                engine.restartFromOrder(order, pendingTx)
                    .flatMap { px ->
                        engine.doExecute(px, secondPassword).updateOrderStatus(order.id)
                    }
            }
}
