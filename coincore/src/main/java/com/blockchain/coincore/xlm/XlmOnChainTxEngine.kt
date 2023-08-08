package com.blockchain.coincore.xlm

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
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.walletoptions.WalletOptionsDataManager
import com.blockchain.fees.FeeType
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.sunriver.Memo
import com.blockchain.sunriver.SendDetails
import com.blockchain.sunriver.XlmAccountReference
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.utils.then
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

const val STATE_MEMO = "XLM_MEMO"

private val PendingTx.memo: TxConfirmationValue.Memo
    get() = (this.engineState[STATE_MEMO] as? TxConfirmationValue.Memo)
        ?: throw IllegalStateException("XLM memo option null")

private fun PendingTx.setMemo(memo: TxConfirmationValue.Memo): PendingTx =
    this.copy(
        engineState = engineState.copyAndPut(STATE_MEMO, memo)
    )

class XlmOnChainTxEngine(
    private val xlmDataManager: XlmDataManager,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    requireSecondPassword: Boolean,
    walletPreferences: WalletStatusPrefs,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(requireSecondPassword, walletPreferences, resolvedAddress) {

    private val targetXlmAddress: XlmAddress
        get() = txTarget as XlmAddress

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check((txTarget as CryptoAddress).asset == CryptoCurrency.XLM)
        check(sourceAsset == CryptoCurrency.XLM)
    }

    override fun doAfterOnRestart(txTarget: TransactionTarget, pendingTx: PendingTx): Single<PendingTx> {
        return super.doAfterOnRestart(txTarget, pendingTx).flatMap { px ->
            isMemoRequired().map { isMemoRequired ->
                targetXlmAddress.memo?.let {
                    px.setMemo(
                        TxConfirmationValue.Memo(
                            text = it,
                            isRequired = isMemoRequired,
                            id = null
                        )
                    )
                } ?: px
            }
        }
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        isMemoRequired().map { memoReq ->
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset),
                feeAmount = Money.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = sourceAsset as AssetInfo
                ),
                selectedFiat = userFiat
            ).setMemo(
                TxConfirmationValue.Memo(
                    text = targetXlmAddress.memo,
                    isRequired = memoReq,
                    id = null
                )
            )
        }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == CryptoCurrency.XLM)

        return Single.zip(
            sourceAccount.balanceRx().firstOrError(),
            absoluteFee()
        ) { balance, fees ->
            pendingTx.copy(
                amount = amount,
                totalBalance = balance.total,
                availableBalance = Money.max(
                    balance.withdrawable - fees,
                    CryptoValue.zero(CryptoCurrency.XLM)
                ) as CryptoValue,
                feeForFullAvailable = fees,
                feeAmount = fees,
                feeSelection = pendingTx.feeSelection.copy(
                    feesForLevels = mapOf(FeeLevel.Regular to fees)
                )
            )
        }
    }

    private fun absoluteFee(): Single<CryptoValue> =
        xlmFeesFetcher.operationFee(FeeType.Regular)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount <= Money.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Singles.zip(
            sourceAccount.balanceRx().firstOrError().map { it.withdrawable },
            absoluteFee()
        ) { balance: Money, fee: Money ->
            if (fee + pendingTx.amount > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            } else {
                true
            }
        }.ignoreElement()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            pendingTx.copy(
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.To(
                        txTarget,
                        AssetAction.Send,
                        sourceAccount
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                            FeeInfo(
                                pendingTx.feeAmount,
                                pendingTx.feeAmount.toUserFiat(exchangeRates),
                                sourceAsset
                            )
                        } else {
                            null
                        },
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    TxConfirmationValue.Total(
                        totalWithFee = (pendingTx.amount as CryptoValue).plus(
                            pendingTx.feeAmount as CryptoValue
                        ),
                        exchange = pendingTx.amount.toUserFiat(exchangeRates)
                            .plus(pendingTx.feeAmount.toUserFiat(exchangeRates))
                    ),
                    pendingTx.memo
                )
            )
        )

    override fun doOptionUpdateRequest(pendingTx: PendingTx, newConfirmation: TxConfirmationValue): Single<PendingTx> {
        return super.doOptionUpdateRequest(pendingTx, newConfirmation)
            .flatMap { tx ->
                (newConfirmation as? TxConfirmationValue.Memo)?.let {
                    Single.just(tx.setMemo(newConfirmation))
                } ?: Single.just(tx)
            }
    }

    private fun isMemoRequired(): Single<Boolean> =
        walletOptionsDataManager.isXlmAddressExchange(targetXlmAddress.address)

    private fun isMemoValid(memoConfirmation: TxConfirmationValue.Memo): Single<Boolean> {
        return isMemoRequired().map {
            if (!it) {
                true
            } else {
                !memoConfirmation.text.isNullOrEmpty() && memoConfirmation.text.length in 1..28 ||
                    memoConfirmation.id != null
            }
        }
    }

    private fun validateOptions(pendingTx: PendingTx): Completable =
        isMemoValid(getMemoOption(pendingTx)).map {
            if (!it) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }
        }.ignoreElement()

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddress()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateOptions(pendingTx) }
            .then { validateDryRun(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun validateAddress() =
        Completable.fromCallable {
            if (!xlmDataManager.isAddressValid(targetXlmAddress.address)) {
                throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
            }
        }

    private fun validateDryRun(pendingTx: PendingTx): Completable =
        createTransaction(pendingTx).flatMap { sendDetails ->
            xlmDataManager.dryRunSendFunds(
                sendDetails
            )
        }.map {
            when (it.errorCode) {
                UNKNOWN_ERROR -> throw TransactionError.ExecutionFailed
                BELOW_MIN_SEND -> throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
                BELOW_MIN_NEW_ACCOUNT -> throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
                INSUFFICIENT_FUNDS -> throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
                BAD_DESTINATION_ACCOUNT_ID -> throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
                SUCCESS -> {
                    // do nothing
                }

                else -> throw TransactionError.ExecutionFailed
            }
        }.ignoreElement()

    private fun getMemoOption(pendingTx: PendingTx) =
        pendingTx.memo

    private fun TxConfirmationValue.Memo.toXlmMemo(): Memo =
        if (!this.text.isNullOrEmpty()) {
            Memo(this.text, Memo.MEMO_TYPE_TEXT)
        } else if (this.id != null) {
            Memo(this.id.toString(), Memo.MEMO_TYPE_ID)
        } else {
            Memo("")
        }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        createTransaction(pendingTx).flatMap { sendDetails ->
            xlmDataManager.sendFunds(sendDetails, secondPassword)
        }.onErrorResumeNext {
            Single.error(TransactionError.ExecutionFailed)
        }.map {
            TxResult.HashedTxResult(it.txHash, pendingTx.amount)
        }

    private fun createTransaction(pendingTx: PendingTx): Single<SendDetails> =
        sourceAccount.receiveAddress.map { receiveAddress ->
            SendDetails(
                from = XlmAccountReference(
                    label = sourceAccount.label,
                    accountId = (receiveAddress as XlmAddress).address,
                    pubKey = null
                ),
                value = pendingTx.amount as CryptoValue,
                toAddress = targetXlmAddress.address,
                toLabel = "",
                fee = pendingTx.feeAmount as CryptoValue,
                memo = getMemoOption(pendingTx).toXlmMemo()
            )
        }

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)

        // These map 1:1 to FailureReason enum class in HorizonProxy
        const val SUCCESS = 0
        const val UNKNOWN_ERROR = 1

        /**
         * The amount attempted to be sent was below that which we allow.
         */
        const val BELOW_MIN_SEND = 2

        /**
         * The destination does exist and a send was attempted that did not fund it
         * with at least the minimum balance for an Horizon account.
         */
        const val BELOW_MIN_NEW_ACCOUNT = 3

        /**
         * The amount attempted to be sent would not leave the source account with at
         * least the minimum balance required for an Horizon account.
         */
        const val INSUFFICIENT_FUNDS = 4

        /**
         * The destination account id is not valid.
         */
        const val BAD_DESTINATION_ACCOUNT_ID = 5
    }
}
