package com.blockchain.coincore.impl.txEngine.swap

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
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
import com.blockchain.nabu.UserIdentity
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class OnChainSwapTxEngine(
    quotesEngine: TransferQuotesEngine,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val userIdentity: UserIdentity,
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val engine: OnChainTxEngineBase
) : SwapTxEngineBase(
    quotesEngine, userIdentity, walletManager
) {
    override val direction: TransferDirection by unsafeLazy {
        when (txTarget) {
            is CustodialTradingAccount -> TransferDirection.FROM_USERKEY
            is CryptoNonCustodialAccount -> TransferDirection.ON_CHAIN
            else -> throw IllegalStateException("Illegal target for on-chain swap engine")
        }
    }

    override val availableBalance: Single<Money>
        get() = sourceAccount.accountBalance

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        if (direction == TransferDirection.ON_CHAIN) {
            check(txTarget is CryptoNonCustodialAccount)
        } else {
            check(txTarget is CustodialTradingAccount)
        }
        check(sourceAsset != (txTarget as CryptoAccount).asset)
        // TODO: Re-enable this once start() has been refactored to be Completable
        //        engine.assertInputsValid()
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        quotesEngine.pricedQuote
            .firstOrError()
            .doOnSuccess { pricedQuote ->
                engine.startFromQuote(pricedQuote)
            }.flatMap { quote ->
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
                    amount = CryptoValue.zero(sourceAsset),
                    totalBalance = CryptoValue.zero(sourceAsset),
                    availableBalance = CryptoValue.zero(sourceAsset),
                    feeForFullAvailable = CryptoValue.zero(sourceAsset),
                    feeAmount = CryptoValue.zero(sourceAsset),
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
}