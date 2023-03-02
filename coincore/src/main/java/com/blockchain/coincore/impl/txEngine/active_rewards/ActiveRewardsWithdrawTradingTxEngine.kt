package com.blockchain.coincore.impl.txEngine.active_rewards

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
import com.blockchain.earn.data.dataresources.active.ActiveRewardsBalanceStore
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class ActiveRewardsWithdrawTradingTxEngine(
    private val activeRewardsBalanceStore: ActiveRewardsBalanceStore,
    private val activeRewardsService: ActiveRewardsService,
    private val tradingStore: TradingStore,
    private val walletManager: CustodialWalletManager,
) : ActiveRewardsBaseEngine(activeRewardsService) {
    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(activeRewardsBalanceStore, tradingStore)

    private val availableBalance: Single<Money>
        get() = sourceAccount.balanceRx().firstOrError().map { it.withdrawable }

    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    override fun assertInputsValid() {
        check(sourceAccount is EarnRewardsAccount.Active)
        check(txTarget is CryptoAccount)
        check(txTarget is CustodialTradingAccount)
        check(sourceAsset == (txTarget as CryptoAccount).currency)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        availableBalance.map { balance ->
            PendingTx(
                amount = balance,
                limits = TxLimits.fromAmounts(
                    min = balance, // TODO the min/max amounts are the full balance
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
        Single.just(pendingTx)

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        availableBalance.flatMapCompletable { balance ->
            if (pendingTx.amount == balance) {
                Completable.complete()
            } else {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }.updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateAmount(pendingTx).updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget, AssetAction.ActiveRewardsWithdraw, sourceAccount
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

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        walletManager.executeCustodialTransfer(pendingTx.amount, Product.EARN_CC1W, Product.BUY)
            .doOnComplete {
                activeRewardsBalanceStore.invalidate()
            }.toSingle {
                TxResult.UnHashedTxResult(pendingTx.amount)
            }
}
