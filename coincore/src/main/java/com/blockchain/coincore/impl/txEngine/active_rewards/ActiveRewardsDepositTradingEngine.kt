package com.blockchain.coincore.impl.txEngine.active_rewards

import androidx.annotation.VisibleForTesting
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.toCrypto
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.TxLimits
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

const val AR_LIMITS: String = "AR_LIMITS"

class ActiveRewardsDepositTradingEngine(
    private val activeRewardsBalanceStore: FlushableDataSource,
    activeRewardsService: ActiveRewardsService,
    private val tradingStore: TradingStore,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager
) : ActiveRewardsBaseEngine(activeRewardsService) {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(activeRewardsBalanceStore, tradingStore, paymentTransactionHistoryStore)

    override fun assertInputsValid() {
        check(sourceAccount is TradingAccount)
        check(txTarget is EarnRewardsAccount.Active)
        check(txTarget is CryptoAccount)
        check(sourceAsset == (txTarget as CryptoAccount).currency)
    }

    private val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map { it.total }

    override fun doInitialiseTx(): Single<PendingTx> {
        return Single.zip(
            getLimits(),
            availableBalance
        ) { (asset, arLimits), balance ->
            PendingTx(
                amount = Money.zero(sourceAsset),
                limits = TxLimits.withMinAndUnlimitedMax(
                    arLimits.minDepositValue.toCrypto(exchangeRates, asset)
                ),
                feeSelection = FeeSelection(),
                selectedFiat = userFiat,
                availableBalance = balance,
                totalBalance = balance,
                feeAmount = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset),
                engineState = mapOf(AR_LIMITS to arLimits)
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
            modifyEngineConfirmations(
                pendingTx = pendingTx
            )
        }

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> {
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            buildConfirmations(pendingTx)
        ).flatMap {
            modifyEngineConfirmations(it)
        }

    private fun buildConfirmations(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(
            txConfirmations = listOfNotNull(
                TxConfirmationValue.From(sourceAccount, sourceAsset),
                TxConfirmationValue.To(
                    txTarget = txTarget,
                    assetAction = AssetAction.ActiveRewardsDeposit,
                    sourceAccount = sourceAccount
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
        return areOptionsValid(pendingTx).map { optionsValid ->
            if (!optionsValid) {
                pendingTx.copy(validationState = ValidationState.OPTION_INVALID)
            } else {
                pendingTx.copy(validationState = ValidationState.CAN_EXECUTE)
            }
        }
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.executeCustodialTransfer(
            amount = pendingTx.amount,
            origin = Product.BUY,
            destination = Product.EARN_CC1W
        ).doOnComplete {
            activeRewardsBalanceStore.invalidate()
        }.toSingle {
            TxResult.UnHashedTxResult(pendingTx.amount)
        }
}
