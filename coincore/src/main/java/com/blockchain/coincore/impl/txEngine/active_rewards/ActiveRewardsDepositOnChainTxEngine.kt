package com.blockchain.coincore.impl.txEngine.active_rewards

import androidx.annotation.VisibleForTesting
import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toCrypto
import com.blockchain.core.history.data.datasources.PaymentTransactionHistoryStore
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.earn.data.dataresources.active.ActiveRewardsBalanceStore
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.store.Store
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class ActiveRewardsDepositOnChainTxEngine(
    private val activeRewardsBalanceStore: ActiveRewardsBalanceStore,
    activeRewardsService: ActiveRewardsService,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val onChainEngine: OnChainTxEngineBase,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val walletManager: CustodialWalletManager
) : ActiveRewardsBaseEngine(activeRewardsService) {

    private val paymentTransactionHistoryStore: PaymentTransactionHistoryStore by scopedInject()

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(activeRewardsBalanceStore, paymentTransactionHistoryStore)

    private val balancesCache: Store<BalancesResponse> by scopedInject()

    override fun assertInputsValid() {
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is ReceiveAddress)

        // TODO: Re-enable this once start() has been refactored to be Completable
        // We have to pass the receiveAddress here cause we need to start the onchain engine with that
        // and so we need a way to get the receiveAddress from the CryptoInterestAccount.
        // This will be possible when start() returns a completable
        // check(sourceAccount.asset == (txTarget as CryptoInterestAccount).asset)
        // check(txTarget is CryptoInterestAccount)
        // onChainEngine.assertInputsValid()
    }

    override fun doAfterOnStart(
        sourceAccount: BlockchainAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRatesDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        onChainEngine.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
    }

    override fun ensureSourceBalanceFreshness() {
        balancesCache.markAsStale()
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        onChainEngine.doInitialiseTx()
            .flatMap { pendingTx ->
                getLimits()
                    .map { (asset, arLimits) ->
                        pendingTx.copy(
                            limits = TxLimits.withMinAndUnlimitedMax(
                                arLimits.minDepositValue.toCrypto(exchangeRates, asset)
                            ),
                            feeSelection = pendingTx.feeSelection.copy(
                                selectedLevel = FeeLevel.Regular,
                                availableLevels = AVAILABLE_FEE_LEVELS
                            ),
                            engineState = mapOf(AR_LIMITS to arLimits)
                        )
                    }
            }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doUpdateAmount(amount, pendingTx)

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        return Single.just(pendingTx)
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doBuildConfirmations(pendingTx).map { pTx ->
            modifyEngineConfirmations(pTx)
        }.flatMap { px ->
            if (px.hasOption(TxConfirmation.MEMO)) {
                px.getOption<TxConfirmationValue.Memo>(TxConfirmation.MEMO)?.let { memo ->
                    onChainEngine.doOptionUpdateRequest(px, memo.copy(editable = false))
                } ?: Single.just(px)
            } else {
                Single.just(px)
            }
        }

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> =
        if (newConfirmation.confirmation in setOf(
                TxConfirmation.AGREEMENT_BLOCKCHAIN_T_AND_C,
                TxConfirmation.AGREEMENT_ACTIVE_REWARDS_TRANSFER,
                TxConfirmation.AGREEMENT_ACTIVE_REWARDS_WITHDRAWAL_DISABLED
            )
        ) {
            Single.just(pendingTx.addOrReplaceOption(newConfirmation))
        } else {
            onChainEngine.doOptionUpdateRequest(pendingTx, newConfirmation)
                .map { pTx ->
                    modifyEngineConfirmations(
                        pendingTx = pTx
                    )
                }
        }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doValidateAmount(pendingTx)
            .map {
                if (it.amount.isPositive && it.isMinLimitViolated()) {
                    it.copy(validationState = ValidationState.UNDER_MIN_LIMIT)
                } else {
                    it
                }
            }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        onChainEngine.doValidateAll(pendingTx)
            .map {
                if (it.validationState == ValidationState.CAN_EXECUTE && !areOptionsValid(pendingTx)) {
                    it.copy(validationState = ValidationState.OPTION_INVALID)
                } else {
                    it
                }
            }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        onChainEngine.doExecute(pendingTx, secondPassword)
            .doOnSuccess { activeRewardsBalanceStore.invalidate() }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        onChainEngine.doPostExecute(pendingTx, txResult)

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
