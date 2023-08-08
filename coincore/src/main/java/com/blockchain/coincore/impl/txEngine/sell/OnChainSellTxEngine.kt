package com.blockchain.coincore.impl.txEngine.sell

import com.blockchain.api.selfcustody.BalancesResponse
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
import com.blockchain.core.TransactionsStore
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialSwapActivityStore
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.then
import info.blockchain.balance.Money
import info.blockchain.balance.isLayer2Token
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Observables

class OnChainSellTxEngine(
    private val tradingStore: TradingStore,
    private val engine: OnChainTxEngineBase,
    walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    userIdentity: UserIdentity,
    quotesEngine: TransferQuotesEngine
) : SellTxEngineBase(
    walletManager,
    limitsDataManager,
    userIdentity,
    quotesEngine
) {
    private val swapActivityStore: CustodialSwapActivityStore by scopedInject()
    private val transactionsStore: TransactionsStore by scopedInject()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(tradingStore, swapActivityStore, transactionsStore)

    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    override val direction: TransferDirection
        get() = TransferDirection.FROM_USERKEY

    override val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map {
            it.total
        }

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is FiatAccount)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Observables.combineLatest(
            quotesEngine.getPriceQuote(),
            quotesEngine.getSampleDepositAddress().toObservable()
        )
            .firstOrError()
            .doOnSuccess { (_, sampleDepositAddress) ->
                engine.startFromTargetAddress(sampleDepositAddress)
            }.flatMap { (quote, _) ->
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
        if (sourceAsset.isLayer2Token) {
            Money.zero(sourceAsset)
        } else pendingTx.feeAmount

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

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .then { engine.doPostExecute(pendingTx, txResult) }
}
