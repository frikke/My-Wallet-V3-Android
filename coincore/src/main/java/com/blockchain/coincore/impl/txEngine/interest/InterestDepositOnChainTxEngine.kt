package com.blockchain.coincore.impl.txEngine.interest

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
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.storedatasource.FlushableDataSource
import com.google.common.annotations.VisibleForTesting
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class InterestDepositOnChainTxEngine(
    private val interestBalanceStore: FlushableDataSource,
    interestService: InterestService,
    @get:VisibleForTesting
    val onChainEngine: OnChainTxEngineBase,
    @get:VisibleForTesting
    val walletManager: CustodialWalletManager
) : InterestBaseEngine(interestService) {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf(interestBalanceStore, paymentTransactionHistoryStore)

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

    override fun doInitialiseTx(): Single<PendingTx> =
        onChainEngine.doInitialiseTx()
            .flatMap { pendingTx ->
                getLimits()
                    .map { (asset, interestLimits) ->
                        pendingTx.copy(
                            limits = TxLimits.withMinAndUnlimitedMax(
                                interestLimits.minDepositFiatValue.toCrypto(exchangeRates, asset)
                            ),
                            feeSelection = pendingTx.feeSelection.copy(
                                selectedLevel = FeeLevel.Regular,
                                availableLevels = AVAILABLE_FEE_LEVELS
                            )
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
                TxConfirmation.AGREEMENT_INTEREST_TRANSFER
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
            .doOnSuccess { interestBalanceStore.invalidate() }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        onChainEngine.doPostExecute(pendingTx, txResult)

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
    }
}
