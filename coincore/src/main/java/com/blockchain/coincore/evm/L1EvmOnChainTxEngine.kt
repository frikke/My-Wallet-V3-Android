package com.blockchain.coincore.evm

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber

class L1EvmOnChainTxEngine(
    private val erc20DataManager: Erc20DataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatusPrefs,
    requireSecondPassword: Boolean,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences,
    resolvedAddress
) {

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAssetInfo),
                feeAmount = Money.zero(sourceAssetInfo),
                feeSelection = FeeSelection(
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAssetInfo)),
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = sourceAssetInfo
                ),
                selectedFiat = userFiat
            )
        )

    private fun buildConfirmationTotal(pendingTx: PendingTx): TxConfirmationValue.Total {
        val fiatAmount = pendingTx.amount.toUserFiat(exchangeRates) as FiatValue

        return TxConfirmationValue.Total(
            totalWithFee = pendingTx.amount,
            exchange = fiatAmount
        )
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                confirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget, AssetAction.Send, sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toUserFiat(exchangeRates),
                                sourceAsset,
                                (sourceAccount as? L1EvmNonCustodialAccount)?.l1Network
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel,
                        ignoreErc20LinkedNote = true
                    ),
                    buildConfirmationTotal(pendingTx)
                )
            )
        )

    private fun absoluteFees(): Single<Map<FeeLevel, CryptoValue>> =
        feeOptions().map { feeOptions ->
            val gasLimit = if (txTarget.isContract) { feeOptions.gasLimitContract } else { feeOptions.gasLimit }
            mapOf(
                FeeLevel.None to CryptoValue.zero(sourceAssetInfo),
                FeeLevel.Regular to getValueForFeeLevel(gasLimit, feeOptions.regularFee),
                FeeLevel.Priority to getValueForFeeLevel(gasLimit, feeOptions.priorityFee),
                FeeLevel.Custom to getValueForFeeLevel(gasLimit, feeOptions.priorityFee)
            )
        }

    private fun getValueForFeeLevel(gasLimitContract: Long, feeLevel: Long) =
        CryptoValue.fromMinor(
            sourceAssetInfo,
            Convert.toWei(
                BigDecimal.valueOf(gasLimitContract * feeLevel),
                Convert.Unit.GWEI
            )
        )

    private fun FeeOptions.mapFeeLevel(feeLevel: FeeLevel) =
        when (feeLevel) {
            FeeLevel.None -> 0L
            FeeLevel.Regular -> regularFee
            FeeLevel.Priority,
            FeeLevel.Custom -> priorityFee
        }

    private val sourceAssetInfo: AssetInfo
        get() = sourceAsset as AssetInfo

    private val evmNetworkTicker: String
        get() = (sourceAccount as? L1EvmNonCustodialAccount)?.l1Network?.networkTicker
            ?: sourceAssetInfo.networkTicker

    private fun feeOptions(): Single<FeeOptions> =
        if (sourceAssetInfo.networkTicker == CryptoCurrency.MATIC.networkTicker) {
            feeManager.getEvmFeeOptions(evmNetworkTicker).singleOrError()
        } else {
            // Once MATIC is migrated onto the new endpoint, remember that the suffix needs to be removed from its ticker
            erc20DataManager.getFeesForEvmTransaction(evmNetworkTicker)
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)
        return Single.zip(
            sourceAccount.balanceRx.firstOrError(),
            absoluteFees()
        ) { balance, feesForLevels ->
            val fee = feesForLevels[pendingTx.feeSelection.selectedLevel] ?: CryptoValue.zero(sourceAssetInfo)

            pendingTx.copy(
                amount = amount,
                totalBalance = balance.total,
                availableBalance = balance.withdrawable,
                feeForFullAvailable = fee,
                feeAmount = fee,
                feeSelection = pendingTx.feeSelection.copy(
                    feesForLevels = feesForLevels
                )
            )
        }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateSufficientGas(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddresses()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateSufficientGas(pendingTx) }
            .then { validateNoPendingTx() }
            .updateTxValidity(pendingTx)

    // This should have already been checked, but we'll check again because
    // burning tokens by sending them to the contract address is probably not what we
    // want to do
    private fun validateAddresses(): Completable {
        val tgt = txTarget as CryptoAddress

        return erc20DataManager.isContractAddress(
            address = tgt.address,
            l1Chain = evmNetworkTicker
        )
            .map { isContract ->
                if (isContract || tgt !is L1EvmAddress) {
                    throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
                } else {
                    isContract
                }
            }.ignoreElement()
    }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= Money.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        sourceAccount.balanceRx.firstOrError().map { it.withdrawable }
            .map { balance ->
                if (pendingTx.amount > balance) {
                    throw TxValidationFailure(
                        ValidationState.INSUFFICIENT_FUNDS
                    )
                } else {
                    true
                }
            }.ignoreElement()

    private fun validateSufficientGas(pendingTx: PendingTx): Completable =
        Single.zip(
            sourceAccount.balanceRx.map { it.total }.firstOrError(),
            absoluteFees()
        ) { balance, feeLevels ->
            val fee = feeLevels[pendingTx.feeSelection.selectedLevel] ?: CryptoValue.zero(sourceAssetInfo)

            if (fee > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_GAS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateNoPendingTx() =
        erc20DataManager.hasUnconfirmedTransactions()
            .flatMapCompletable { hasUnconfirmed: Boolean ->
                if (hasUnconfirmed) {
                    Completable.error(
                        TxValidationFailure(
                            ValidationState.HAS_TX_IN_FLIGHT
                        )
                    )
                } else {
                    Completable.complete()
                }
            }

    override fun doExecute(
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<TxResult> =
        createTransaction(pendingTx)
            .flatMap {
                erc20DataManager.signErc20Transaction(
                    it,
                    secondPassword,
                    evmNetworkTicker
                )
            }
            .flatMap {
                erc20DataManager.pushErc20Transaction(it, evmNetworkTicker)
            }
            .onErrorResumeNext {
                Timber.e(it)
                Single.error(TransactionError.ExecutionFailed)
            }.map { hash ->
                TxResult.HashedTxResult(hash, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        val tgt = txTarget as CryptoAddress

        return Singles.zip(
            feeOptions(),
            resolvedHotWalletAddress
        )
            .flatMap { (fees, hotWalletAddress) ->
                erc20DataManager.createEvmTransaction(
                    asset = sourceAssetInfo,
                    to = tgt.address,
                    amount = pendingTx.amount.toBigInteger(),
                    gasPriceWei = fees.gasPrice(
                        pendingTx.feeSelection.selectedLevel
                    ),
                    gasLimitGwei = fees.gasLimitGwei,
                    hotWalletAddress = hotWalletAddress,
                    evmNetwork = evmNetworkTicker
                )
            }
    }

    private fun FeeOptions.gasPrice(feeLevel: FeeLevel): BigInteger =
        Convert.toWei(
            BigDecimal.valueOf(this.mapFeeLevel(feeLevel)),
            Convert.Unit.GWEI
        ).toBigInteger()

    private val FeeOptions.gasLimitGwei: BigInteger
        get() = BigInteger.valueOf(
            gasLimitContract
        )

    private val TransactionTarget.isContract: Boolean
        get() = (this as? L1EvmAddress)?.isContract ?: false

    companion object {
        private val AVAILABLE_FEE_LEVELS =
            setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
