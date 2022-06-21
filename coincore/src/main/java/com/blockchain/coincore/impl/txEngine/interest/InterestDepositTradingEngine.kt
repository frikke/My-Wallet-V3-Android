package com.blockchain.coincore.impl.txEngine.interest

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.toCrypto
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.interest.domain.InterestStoreService
import com.blockchain.core.limits.TxLimits
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class InterestDepositTradingEngine(
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val interestBalances: InterestBalanceDataManager,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val interestStoreService: InterestStoreService,
) : InterestBaseEngine(walletManager) {

    override fun assertInputsValid() {
        check(sourceAccount is TradingAccount)
        check(txTarget is InterestAccount)
        check(txTarget is CryptoAccount)
        check(sourceAsset == (txTarget as CryptoAccount).currency)
    }

    private val availableBalance: Single<Money>
        get() = sourceAccount.balance.firstOrError().map { it.total }

    override fun doInitialiseTx(): Single<PendingTx> {
        return Single.zip(
            getLimits(),
            availableBalance
        ) { limits, balance ->
            val cryptoAsset = limits.cryptoCurrency
            PendingTx(
                amount = Money.zero(sourceAsset),
                limits = TxLimits.withMinAndUnlimitedMax(
                    limits.minDepositFiatValue.toCrypto(exchangeRates, cryptoAsset)
                ),
                feeSelection = FeeSelection(),
                selectedFiat = userFiat,
                availableBalance = balance,
                totalBalance = balance,
                feeAmount = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset)
            )
        }
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
        }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation.confirmation.isInterestAgreement()) {
            Single.just(pendingTx.addOrReplaceOption(newConfirmation))
        } else {
            Single.just(
                modifyEngineConfirmations(
                    pendingTx = pendingTx
                )
            )
        }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            buildConfirmations(pendingTx)
        ).map {
            modifyEngineConfirmations(it)
        }

    private fun buildConfirmations(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.From(sourceAccount, sourceAsset),
                TxConfirmationValue.To(
                    txTarget, AssetAction.InterestDeposit, sourceAccount
                ),
                TxConfirmationValue.Total(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        pendingTx.feeAmount as CryptoValue
                    ),
                    exchange = pendingTx.amount.toUserFiat(exchangeRates)
                        .plus(pendingTx.feeAmount.toUserFiat(exchangeRates))
                )
            )
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                checkIfAmountIsBelowMinLimit(pendingTx)
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }.updateTxValidity(pendingTx)

    private fun checkIfAmountIsBelowMinLimit(pendingTx: PendingTx) =
        when {
            pendingTx.limits == null -> {
                throw TxValidationFailure(ValidationState.UNINITIALISED)
            }
            pendingTx.isMinLimitViolated() -> throw TxValidationFailure(
                ValidationState.UNDER_MIN_LIMIT
            )
            else -> Completable.complete()
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        val px = if (!areOptionsValid(pendingTx)) {
            pendingTx.copy(validationState = ValidationState.OPTION_INVALID)
        } else {
            pendingTx.copy(validationState = ValidationState.CAN_EXECUTE)
        }
        return Single.just(px)
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.executeCustodialTransfer(
            amount = pendingTx.amount,
            origin = Product.BUY,
            destination = Product.SAVINGS
        ).doOnComplete {
            interestBalances.flushCaches(sourceAssetInfo)
        }.toSingle {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .doOnComplete { interestStoreService.invalidate() }
}
