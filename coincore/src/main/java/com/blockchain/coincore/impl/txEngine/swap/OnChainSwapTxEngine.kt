package com.blockchain.coincore.impl.txEngine.swap

import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.then
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Observables

class OnChainSwapTxEngine(
    quotesEngine: TransferQuotesEngine,
    private val walletManager: CustodialWalletManager,
    limitsDataManager: LimitsDataManager,
    swapTransactionsStore: SwapTransactionsStore,
    private val userIdentity: UserIdentity,
    private val engine: OnChainTxEngineBase
) : SwapTxEngineBase(
    quotesEngine,
    userIdentity,
    walletManager,
    limitsDataManager,
    swapTransactionsStore
) {
    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    override val direction: TransferDirection by unsafeLazy {
        when (txTarget) {
            is CustodialTradingAccount -> TransferDirection.FROM_USERKEY
            is CryptoNonCustodialAccount -> TransferDirection.ON_CHAIN
            else -> throw IllegalStateException("Illegal target for on-chain swap engine")
        }
    }

    override val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map {
            it.total
        }

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        if (direction == TransferDirection.ON_CHAIN) {
            check(txTarget is CryptoNonCustodialAccount)
        } else {
            check(txTarget is CustodialTradingAccount)
        }
        check(sourceAsset != (txTarget as CryptoAccount).currency)
        // TODO: Re-enable this once start() has been refactored to be Completable
        //        engine.assertInputsValid()
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
                        updateLimits(userFiat, it, quote)
                    }
            }.map { px ->
                px.copy(
                    feeSelection = defaultFeeSelection(px)
                )
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

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        return engine.doUpdateAmount(amount, pendingTx)
            .updateQuotePrice()
            .clearConfirmations()
    }

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> = Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAmount(pendingTx)
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
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return engine.doValidateAll(pendingTx)
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
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createOrder(pendingTx)
            .flatMap { order ->
                engine.restartFromOrder(order, pendingTx)
                    .flatMap { px ->
                        engine.doExecute(px, secondPassword)
                            .updateOrderStatus(order.id)
                    }
            }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .then { engine.doPostExecute(pendingTx, txResult) }
}
