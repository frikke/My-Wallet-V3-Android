package com.blockchain.coincore.erc20

import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.then

open class Erc20OnChainTxEngine(
    private val erc20DataManager: Erc20DataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
) {

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAsset as AssetInfo)),
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = CryptoCurrency.ETHER
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
                                sourceAsset
                            )
                        } else null,
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    buildConfirmationTotal(pendingTx),
                    TxConfirmationValue.Description().takeIf { erc20DataManager.supportsErc20TxNote(sourceAssetInfo) }
                )
            )
        )

    private fun absoluteFees(): Single<Map<FeeLevel, CryptoValue>> =
        feeOptions().map {
            val gasLimitContract = it.gasLimitContract
            mapOf(
                FeeLevel.None to CryptoValue.zero(CryptoCurrency.ETHER),
                FeeLevel.Regular to getValueForFeeLevel(gasLimitContract, it.regularFee),
                FeeLevel.Priority to getValueForFeeLevel(gasLimitContract, it.priorityFee),
                FeeLevel.Custom to getValueForFeeLevel(gasLimitContract, it.priorityFee)
            )
        }

    private fun getValueForFeeLevel(gasLimitContract: Long, feeLevel: Long) =
        CryptoValue.fromMinor(
            CryptoCurrency.ETHER,
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

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.getErc20FeeOptions(sourceAssetInfo.l2identifier)
            .singleOrError()

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)
        return Single.zip(
            sourceAccount.balance.firstOrError(),
            absoluteFees()
        ) { balance, feesForLevels ->
            val fee = feesForLevels[pendingTx.feeSelection.selectedLevel] ?: CryptoValue.zero(CryptoCurrency.ETHER)

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

        return erc20DataManager.isContractAddress(tgt.address)
            .map { isContract ->
                if (isContract || tgt !is Erc20Address) {
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
        sourceAccount.balance.firstOrError().map { it.withdrawable }
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
            erc20DataManager.getEthBalance(),
            absoluteFees()
        ) { balance, feeLevels ->
            val fee = feeLevels[pendingTx.feeSelection.selectedLevel] ?: CryptoValue.zero(CryptoCurrency.ETHER)

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
                    secondPassword
                )
            }
            .flatMap { erc20DataManager.pushErc20Transaction(it) }
            .flatMap { hash ->
                pendingTx.getOption<TxConfirmationValue.Description>(
                    TxConfirmation.DESCRIPTION
                )?.let { notes ->
                    erc20DataManager.putErc20TxNote(sourceAssetInfo, hash, notes.text)
                }?.toSingle {
                    hash
                } ?: Single.just(hash)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun createTransaction(pendingTx: PendingTx): Single<RawTransaction> {
        val tgt = txTarget as CryptoAddress

        return feeOptions()
            .flatMap { fees ->
                erc20DataManager.createErc20Transaction(
                    asset = sourceAssetInfo,
                    to = tgt.address,
                    amount = pendingTx.amount.toBigInteger(),
                    gasPriceWei = fees.gasPrice(
                        pendingTx.feeSelection.selectedLevel
                    ),
                    gasLimitGwei = fees.gasLimitGwei
                )
            }
    }

    private fun FeeOptions.gasPrice(feeLevel: FeeLevel): BigInteger =
        Convert.toWei(
            BigDecimal.valueOf(this.mapFeeLevel(feeLevel)),
            Convert.Unit.GWEI
        ).toBigInteger()

    private
    val FeeOptions.gasLimitGwei: BigInteger
        get() = BigInteger.valueOf(
            gasLimitContract
        )

    companion object {
        private val AVAILABLE_FEE_LEVELS =
            setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
