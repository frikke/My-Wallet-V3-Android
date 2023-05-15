package com.blockchain.coincore.impl.txEngine.staking

import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.TxLimits
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger

class StakingWithdrawTradingTxEngine(
    private val stakingService: StakingService,
    private val stakingBalanceStore: FlushableDataSource,
    private val tradingStore: TradingStore,
    private val walletManager: CustodialWalletManager
) : StakingBaseEngine(stakingService) {
    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(stakingBalanceStore, tradingStore, paymentTransactionHistoryStore)

    private val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map { it.withdrawable }

    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    override fun assertInputsValid() {
        check(sourceAccount is EarnRewardsAccount.Staking)
        check(txTarget is CryptoAccount)
        check(txTarget is CustodialTradingAccount)
        check(sourceAsset == (txTarget as CryptoAccount).currency)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        availableBalance.map { balance ->
            PendingTx(
                amount = Money.zero(sourceAsset),
                limits = TxLimits.fromAmounts(
                    min = Money.fromMinor(sourceAsset, BigInteger.ONE),
                    max = balance
                ),
                feeSelection = FeeSelection(),
                selectedFiat = userFiat,
                availableBalance = balance,
                totalBalance = balance,
                feeAmount = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset)
            )
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

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount <= balance) {
                checkIfAmountIsBelowMinLimit(pendingTx)
            } else {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }.updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget,
                        AssetAction.StakingWithdraw,
                        sourceAccount
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
        )

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.executeCustodialTransfer(pendingTx.amount, Product.STAKING, Product.BUY)
            .doOnComplete {
                stakingBalanceStore.invalidate()
            }.toSingle {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
}
