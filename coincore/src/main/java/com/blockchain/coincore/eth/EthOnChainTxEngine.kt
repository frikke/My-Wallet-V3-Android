package com.blockchain.coincore.eth

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.Money.Companion.max
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.utils.extensions.then

class EthOnChainTxEngine(
    private val ethDataManager: EthDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatusPrefs,
    requireSecondPassword: Boolean,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences,
    resolvedAddress
) {

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check((txTarget as CryptoAddress).asset == CryptoCurrency.ETHER)
        check(sourceAsset == CryptoCurrency.ETHER)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset),
                feeAmount = Money.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAsset as AssetInfo)),
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = sourceAsset as AssetInfo
                ),
                selectedFiat = userFiat
            )
        )

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget, AssetAction.Send, sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toUserFiat(exchangeRates),
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    TxConfirmationValue.Total(
                        totalWithFee = (pendingTx.amount as CryptoValue).plus(
                            pendingTx.feeAmount as CryptoValue
                        ),
                        exchange = pendingTx.amount.toUserFiat(exchangeRates)
                            .plus(pendingTx.feeAmount.toUserFiat(exchangeRates))
                    ),
                    TxConfirmationValue.Description()
                )
            )
        )

    private fun absoluteFees(): Single<Map<FeeLevel, CryptoValue>> =
        feeOptions().zipWith(resolvedHotWalletAddress)
            .map { (feeOptions, hotWalletAddress) ->
                val gasLimit = if (txTarget.isContract) feeOptions.gasLimitContract else feeOptions.gasLimit
                val extraGasForMemo = extraGasLimitIfMemoAvailable(
                    useHotWallet = hotWalletAddress.isNotEmpty()
                ).toLong()
                mapOf(
                    FeeLevel.None to CryptoValue.zero(CryptoCurrency.ETHER),
                    FeeLevel.Regular to getValueForFeeLevel(gasLimit, feeOptions.regularFee, extraGasForMemo),
                    FeeLevel.Priority to getValueForFeeLevel(gasLimit, feeOptions.priorityFee, extraGasForMemo),
                    FeeLevel.Custom to getValueForFeeLevel(gasLimit, feeOptions.priorityFee, extraGasForMemo)
                )
            }

    private fun getValueForFeeLevel(gasLimitContract: Long, feeLevel: Long, extraGasForMemo: Long) =
        CryptoValue.fromMinor(
            CryptoCurrency.ETHER,
            Convert.toWei(
                BigDecimal.valueOf(gasLimitContract * feeLevel + extraGasForMemo),
                Convert.Unit.GWEI
            )
        )

    private fun FeeOptions.mapFeeLevel(feeLevel: FeeLevel) =
        when (feeLevel) {
            FeeLevel.None -> 0L
            FeeLevel.Regular -> regularFee
            FeeLevel.Priority -> priorityFee
            FeeLevel.Custom -> priorityFee
        }

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.ethFeeOptions.firstOrError()

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Single.zip(
            sourceAccount.balanceRx.firstOrError(),
            absoluteFees()
        ) { balance, feeLevels ->
            val total = balance.total as CryptoValue
            val available = balance.withdrawable as CryptoValue
            val fees = feeLevels[pendingTx.feeSelection.selectedLevel] ?: Money.zero(sourceAsset)

            pendingTx.copy(
                amount = amount,
                totalBalance = total,
                availableBalance = max(available - fees, Money.zero(sourceAsset)) as CryptoValue,
                feeForFullAvailable = fees,
                feeAmount = fees,
                feeSelection = pendingTx.feeSelection.copy(
                    feesForLevels = feeLevels
                )
            )
        }
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    // We can make some assumptions here over the previous impl;
    // 1. a CryptAddress object will be self-validating, so we need not check that it's valid
    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .then { validateNoPendingTx() }
            .updateTxValidity(pendingTx)

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createTransaction(pendingTx)
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { hash ->
                pendingTx.getOption<TxConfirmationValue.Description>(TxConfirmation.DESCRIPTION)?.let { notes ->
                    ethDataManager.updateTransactionNotes(hash, notes.text)
                }?.toSingle {
                    hash
                } ?: Single.just(hash)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        return Singles.zip(
            ethDataManager.getNonce(),
            feeOptions(),
            resolvedHotWalletAddress
        ).map { (nonce, fees, hotWalletAddress) ->
            val useHotWallet = hotWalletAddress.isNotEmpty()

            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = if (useHotWallet) hotWalletAddress else
                    (txTarget as CryptoAddress).address,
                gasPriceWei = fees.gasPrice(pendingTx.feeSelection.selectedLevel),
                gasLimitGwei = fees.getGasLimit(txTarget.isContract) + extraGasLimitIfMemoAvailable(useHotWallet),
                weiValue = pendingTx.amount.toBigInteger(),
                data = if (useHotWallet) (txTarget as CryptoAddress).address else ""
            )
        }
    }

    private fun extraGasLimitIfMemoAvailable(useHotWallet: Boolean): BigInteger =
        if (useHotWallet) {
            ethDataManager.extraGasLimitForMemo()
        } else {
            BigInteger.ZERO
        }

    // TODO: Have FeeOptions deal with this conversion
    private fun FeeOptions.gasPrice(feeLevel: FeeLevel): BigInteger =
        Convert.toWei(
            BigDecimal.valueOf(this.mapFeeLevel(feeLevel)),
            Convert.Unit.GWEI
        ).toBigInteger()

    private fun FeeOptions.getGasLimit(isContract: Boolean): BigInteger =
        BigInteger.valueOf(
            if (isContract) gasLimitContract else gasLimit
        )

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= Money.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Single.zip(
            sourceAccount.balanceRx.map { it.withdrawable }.firstOrError(),
            absoluteFees()
        ) { balance: Money, feeLevels ->
            val fee = feeLevels[pendingTx.feeSelection.selectedLevel] ?: Money.zero(sourceAsset)

            if (fee + pendingTx.amount > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateNoPendingTx() =
        ethDataManager.isLastTxPending()
            .flatMapCompletable { hasUnconfirmed: Boolean ->
                if (hasUnconfirmed) {
                    Completable.error(TxValidationFailure(ValidationState.HAS_TX_IN_FLIGHT))
                } else {
                    Completable.complete()
                }
            }

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}

private val TransactionTarget.isContract: Boolean
    get() = (this as? EthAddress)?.isContract ?: false
