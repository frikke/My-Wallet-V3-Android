package com.blockchain.transactions.common

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.makeExternalAssetAddress
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.flatMap
import com.blockchain.outcome.map
import com.blockchain.outcome.mapError
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OnChainDepositEngineInteractor(
    private val custodialWalletManager: CustodialWalletManager,
    private val exchangeRatesDataManager: ExchangeRatesDataManager,
) {
    private lateinit var txEngine: OnChainTxEngineBase
    private lateinit var pendingTx: PendingTx
    private var currentSourceAsset: AssetInfo? = null

    private val mutex = Mutex()

    suspend fun getDepositNetworkFee(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ): Outcome<Exception, CryptoValue> = mutex.withLock {
        initialiseDepositTxEngineIfNeeded(sourceAccount, targetAccount)
            .flatMap { getSourceNetworkFee(amount, pendingTx) }
    }

    suspend fun validateAmount(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
        amount: CryptoValue,
    ): Outcome<OnChainDepositInputValidationError, Unit> = mutex.withLock {
        initialiseDepositTxEngineIfNeeded(sourceAccount, targetAccount)
            .flatMap {
                txEngine.doUpdateAmount(amount, pendingTx).awaitOutcome()
                    .doOnSuccess { pendingTx ->
                        this.pendingTx = pendingTx
                    }
            }
            .flatMap { pendingTx ->
                txEngine.doValidateAmount(pendingTx).awaitOutcome()
            }
            .mapError { exception ->
                OnChainDepositInputValidationError.Unknown(exception.localizedMessage)
            }
            .flatMap { pendingTx ->
                val error = pendingTx.validationState.toInputValidationError()
                if (error != null) Outcome.Failure(error)
                else Outcome.Success(Unit)
            }
    }

    private suspend fun initialiseDepositTxEngineIfNeeded(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
    ): Outcome<Exception, Unit> =
        if (currentSourceAsset != sourceAccount.currency) {
            updateDepositTxEngine(sourceAccount, targetAccount)
        } else {
            Outcome.Success(Unit)
        }

    private suspend fun updateDepositTxEngine(
        sourceAccount: CryptoNonCustodialAccount,
        targetAccount: CryptoAccount,
    ): Outcome<Exception, Unit> {
        txEngine = sourceAccount.createTxEngine(targetAccount, AssetAction.Swap) as OnChainTxEngineBase
        return custodialWalletManager.getCustodialAccountAddress(Product.TRADE, sourceAccount.currency)
            .awaitOutcome()
            .flatMap { sampleDepositAddress ->
                txEngine.start(
                    sourceAccount = sourceAccount,
                    txTarget = makeExternalAssetAddress(
                        asset = sourceAccount.currency,
                        address = sampleDepositAddress,
                    ),
                    exchangeRates = exchangeRatesDataManager,
                )
                txEngine.doInitialiseTx().awaitOutcome()
            }.doOnSuccess { pendingTx ->
                this.pendingTx = pendingTx
                currentSourceAsset = sourceAccount.currency
            }.map { Unit }
    }

    private suspend fun getSourceNetworkFee(
        amount: CryptoValue,
        depositPendingTx: PendingTx,
    ): Outcome<Exception, CryptoValue> {
        return txEngine.doUpdateAmount(amount, depositPendingTx).awaitOutcome()
            .doOnSuccess { pendingTx ->
                this.pendingTx = pendingTx
            }
            .map { pendingTx ->
                pendingTx.feeAmount as CryptoValue
            }
    }
}

sealed interface OnChainDepositInputValidationError {
    data class Unknown(val error: String?) : OnChainDepositInputValidationError
    object InsufficientGas : OnChainDepositInputValidationError
    object InsufficientFunds : OnChainDepositInputValidationError
}

private fun ValidationState.toInputValidationError(): OnChainDepositInputValidationError? = when (this) {
    ValidationState.INSUFFICIENT_FUNDS -> OnChainDepositInputValidationError.InsufficientFunds
    ValidationState.INSUFFICIENT_GAS -> OnChainDepositInputValidationError.InsufficientGas
    else -> null
}
