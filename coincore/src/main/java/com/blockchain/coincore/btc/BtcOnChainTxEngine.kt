package com.blockchain.coincore.btc

import com.blockchain.bitpay.BitPayClientEngine
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.EngineTransaction
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeLevelRates
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.copyAndPut
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.logging.Logger
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.then
import com.blockchain.utils.unsafeLazy
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency.Companion.Dollars
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import java.math.BigDecimal
import java.math.BigInteger
import org.bitcoinj.core.Transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spongycastle.util.encoders.Hex

private const val STATE_UTXO = "btc_utxo"
private const val FEE_OPTIONS = "fee_options"

private val PendingTx.utxoBundle: SpendableUnspentOutputs
    get() = (this.engineState[STATE_UTXO] as? SpendableUnspentOutputs) ?: SpendableUnspentOutputs()

private val PendingTx.feeOptions: FeeOptions
    get() = (this.engineState[FEE_OPTIONS] as? FeeOptions) ?: FeeOptions()

private class BtcPreparedTx(
    val btcTx: Transaction
) : EngineTransaction {
    override val encodedMsg: String
        get() = String(Hex.encode(btcTx.bitcoinSerialize()))
    override val msgSize: Int = btcTx.messageSize
    override val txHash: String = btcTx.hashAsString
}

class BtcOnChainTxEngine(
    private val btcDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatusPrefs,
    requireSecondPassword: Boolean,
    resolvedAddress: Single<String>
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences,
    resolvedAddress
),
    BitPayClientEngine,
    KoinComponent {

    override fun assertInputsValid() {
        check(sourceAccount is BtcCryptoWalletAccount)
        check(txTarget is CryptoAddress)
        check((txTarget as CryptoAddress).asset == CryptoCurrency.BTC)
        check(sourceAsset == CryptoCurrency.BTC)
    }

    private val btcTarget: CryptoAddress
        get() = txTarget as CryptoAddress

    private val btcSource: BtcCryptoWalletAccount by unsafeLazy {
        sourceAccount as BtcCryptoWalletAccount
    }

    private val sourceAssetInfo: AssetInfo
        get() = sourceAsset.asAssetInfoOrThrow()

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = Money.zero(sourceAsset),
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                limits = TxLimits.fromAmounts(
                    min = Money.fromMinor(sourceAsset, Payment.DUST),
                    max = Money.fromMinor(sourceAsset, MAX_BTC_AMOUNT)
                ),
                feeForFullAvailable = Money.zero(sourceAsset),
                feeAmount = Money.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAssetInfo)),
                    availableLevels = AVAILABLE_FEE_OPTIONS,
                    asset = sourceAssetInfo
                ),
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.zip(
            sourceAccount.balanceRx().firstOrError().map { it.total as CryptoValue },
            getDynamicFeesPerKb(pendingTx),
            getUnspentApiResponse(btcSource.xpubs)
        ) { total, optionsAndFeesPerKb, coins ->
            updatePendingTxFromAmount(
                amount as CryptoValue,
                total,
                pendingTx,
                optionsAndFeesPerKb.second,
                optionsAndFeesPerKb.first,
                coins
            )
        }.onErrorReturnItem(
            pendingTx.copy(
                validationState = ValidationState.INSUFFICIENT_FUNDS
            )
        )

    private fun getUnspentApiResponse(xpubs: XPubs): Single<List<Utxo>> {
        return sourceAccount.balanceRx().firstOrError().flatMap {
            if (it.total.isPositive) {
                sendDataManager.getUnspentBtcOutputs(xpubs)
                    // If we get here, we should have balance...
                    // but if we have no UTXOs then we have a problem:
                    .map { utxo ->
                        utxo.ifEmpty {
                            throw fatalError(IllegalStateException("No BTC UTXOs found for non-zero balance"))
                        }
                    }
            } else {
                Single.error(IllegalStateException("No BTC funds"))
            }
        }
    }

    private fun getDynamicFeesPerKb(pendingTx: PendingTx): Single<Pair<FeeOptions, Map<FeeLevel, Money>>> =
        feeManager.btcFeeOptions
            .map { feeOptions ->
                Pair(
                    feeOptions,
                    mapOf(
                        FeeLevel.None to Money.zero(sourceAsset),
                        FeeLevel.Regular to feeToCrypto(feeOptions.regularFee),
                        FeeLevel.Priority to feeToCrypto(feeOptions.priorityFee),
                        FeeLevel.Custom to feeToCrypto(pendingTx.feeSelection.customAmount)
                    )
                )
            }.singleOrError()

    private fun feeToCrypto(feePerKb: Long): Money =
        Money.fromMinor(sourceAsset, (feePerKb * 1000).toBigInteger())

    private fun updatePendingTxFromAmount(
        amount: Money,
        balance: Money,
        pendingTx: PendingTx,
        feesPerKb: Map<FeeLevel, Money>,
        feeOptions: FeeOptions,
        coins: List<Utxo>
    ): PendingTx {
        val regularFee = feesPerKb[FeeLevel.Regular]
        val priorityFee = feesPerKb[FeeLevel.Priority]

        require(regularFee != null) { "Regular fee per kb is null" }
        require(priorityFee != null) { "Priority fee per kb is null" }

        val targetOutputType = btcDataManager.getAddressOutputType(btcTarget.address)
        val changeOutputType = btcDataManager.getXpubFormatOutputType(btcSource.xpubs.default.derivation)
        val selectedFeeLevel = pendingTx.feeSelection.selectedLevel

        val feeForLevel = feesPerKb[selectedFeeLevel] ?: Money.zero(sourceAsset)

        val available = sendDataManager.getMaximumAvailable(
            asset = sourceAssetInfo,
            targetOutputType = targetOutputType,
            unspentCoins = coins,
            feePerKb = feeForLevel
        ) // This is total balance, with fees deducted

        val coinsSelectedForRegularFee = sendDataManager.getSpendableCoins(
            unspentCoins = coins,
            targetOutputType = targetOutputType,
            changeOutputType = changeOutputType,
            paymentAmount = amount,
            feePerKb = regularFee
        )

        val coinsSelectedForPriorityFee = sendDataManager.getSpendableCoins(
            unspentCoins = coins,
            targetOutputType = targetOutputType,
            changeOutputType = changeOutputType,
            paymentAmount = amount,
            feePerKb = priorityFee
        )

        val utxoBundle = when (selectedFeeLevel) {
            FeeLevel.Priority -> coinsSelectedForPriorityFee
            FeeLevel.Regular -> coinsSelectedForRegularFee
            else -> sendDataManager.getSpendableCoins(
                unspentCoins = coins,
                targetOutputType = targetOutputType,
                changeOutputType = changeOutputType,
                paymentAmount = amount,
                feePerKb = feeForLevel
            )
        }

        val updatedFees = feesPerKb.mapValues {
            when (it.key) {
                FeeLevel.None -> it.value
                FeeLevel.Regular -> Money.fromMinor(sourceAsset, coinsSelectedForRegularFee.absoluteFee)
                FeeLevel.Priority -> Money.fromMinor(sourceAsset, coinsSelectedForPriorityFee.absoluteFee)
                FeeLevel.Custom -> it.value
            }
        }

        return pendingTx.copy(
            amount = amount,
            totalBalance = balance,
            availableBalance = available.maxSpendable,
            feeForFullAvailable = available.feeForMax,
            feeAmount = Money.fromMinor(sourceAsset, utxoBundle.absoluteFee),
            feeSelection = pendingTx.feeSelection.copy(
                customLevelRates = feeOptions.toLevelRates(),
                feesForLevels = updatedFees
            ),
            engineState = pendingTx.engineState
                .copyAndPut(STATE_UTXO, utxoBundle)
                .copyAndPut(FEE_OPTIONS, feeOptions)
        ).let {
            it.copy(
                feeSelection = it.feeSelection.copy(
                    feeState = getFeeState(it, it.feeOptions)
                )
            )
        }
    }

    private fun FeeOptions.toLevelRates(): FeeLevelRates =
        FeeLevelRates(
            regularFee = regularFee,
            priorityFee = priorityFee
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doBuildConfirmations(
        pendingTx: PendingTx
    ): Single<PendingTx> = isLargeTransaction(pendingTx).map { isLargeTransaction ->
        pendingTx.copy(
            txConfirmations = listOfNotNull(
                TxConfirmationValue.From(sourceAccount, sourceAsset),
                TxConfirmationValue.To(txTarget, AssetAction.Send, sourceAccount),
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
                TxConfirmationValue.Description(),
                if (isLargeTransaction) {
                    TxConfirmationValue.TxBooleanConfirmation<Unit>(
                        TxConfirmation.LARGE_TRANSACTION_WARNING
                    )
                } else {
                    null
                }
            )
        )
    }

    // Returns true if bitcoin transaction is large by checking against 3 criteria:
    //  * If the fee > $0.50 AND
    //  * the Tx size is over 1kB AND
    //  * the ratio of fee/amount is over 1%
    private fun isLargeTransaction(pendingTx: PendingTx): Single<Boolean> =
        exchangeRates.exchangeRateLegacy(pendingTx.feeAmount.currency, Dollars)
            .firstOrError()
            .map { exchangeRate ->
                val fiatValue = exchangeRate.convert(pendingTx.feeAmount)
                val outputs = listOf(
                    btcDataManager.getAddressOutputType(btcTarget.address),
                    btcDataManager.getXpubFormatOutputType(btcSource.xpubs.default.derivation)
                )

                val txSize = sendDataManager.estimateSize(
                    inputs = pendingTx.utxoBundle.spendableOutputs,
                    outputs = outputs // assumes change required
                )

                val relativeFee =
                    BigDecimal(100) * (pendingTx.feeAmount.toBigDecimal() / pendingTx.amount.toBigDecimal())
                fiatValue.toBigDecimal() > BigDecimal(LARGE_TX_FEE) &&
                    txSize > LARGE_TX_SIZE &&
                    relativeFee > LARGE_TX_PERCENTAGE
            }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddress()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateOptions(pendingTx) }
            .logValidityFailure()
            .updateTxValidity(pendingTx)

    private fun validateAddress(): Completable =
        Completable.fromCallable {
            if (!FormatsUtil.isValidBitcoinAddress(btcTarget.address)) {
                throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
            }
        }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            val amount = pendingTx.amount.toBigInteger()
            if (amount < Payment.DUST) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount > MAX_BTC_AMOUNT) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount <= BigInteger.ZERO) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.availableBalance < pendingTx.amount) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }

            if (pendingTx.utxoBundle.spendableOutputs.isEmpty()) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }

    private fun validateOptions(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            // If the large_fee warning is present, make sure it's ack'd.
            // If it's not, then there's nothing to do
            if (pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
                    TxConfirmation.LARGE_TRANSACTION_WARNING
                )?.value == false
            ) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }

            if (pendingTx.feeSelection.selectedLevel == FeeLevel.Custom) {
                when {
                    pendingTx.feeSelection.customAmount == -1L ->
                        throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
                    pendingTx.feeSelection.customAmount < MINIMUM_CUSTOM_FEE ->
                        throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
                }
            }
        }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        doPrepareTransaction(pendingTx)
            .flatMap { (tx, _) ->
                doSignTransaction(tx, pendingTx, secondPassword)
            }.flatMap { engineTx ->
                val btcTx = engineTx as BtcPreparedTx
                sendDataManager.submitBtcPayment(btcTx.btcTx)
            }.doOnSuccess {
                doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                doOnTransactionFailed(pendingTx, e)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    // Logic to decide on sending change to bech32 xpub change or receive chain.
    // When moving from legacy to segwit, we should send change to at least one receive address before we start
    // using change address. The BE cannot determine balances held in change address on a derivation chain without
    // at least one receive address having a value.
    // A better way of doing this is to see if we have a +ve change address index, but the routing to have that
    // information available here is complex, and this will work. We should re-visit this when BTC downstack refactoring
    // makes it more sensible.
    private fun selectAddressForChange(
        inputs: SpendableUnspentOutputs,
        changeAddress: String,
        receiveAddress: String
    ): String =
        changeAddress.takeIf { inputs.spendableOutputs.firstOrNull { it.isSegwit } != null } ?: receiveAddress

    override fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction> =
        btcSource.getSigningKeys(
            pendingTx.utxoBundle,
            secondPassword
        )
            .map { keys ->
                BtcPreparedTx(
                    sendDataManager.getSignedBtcTransaction(
                        tx,
                        keys
                    )
                )
            }

    override fun doPrepareTransaction(
        pendingTx: PendingTx
    ): Single<Pair<Transaction, DustInput?>> =
        Singles.zip(
            btcSource.getChangeAddress(),
            btcSource.receiveAddress
        ).map { (changeAddress, receiveAddress) ->
            sendDataManager.getBtcTransaction(
                pendingTx.utxoBundle,
                btcTarget.address,
                selectAddressForChange(pendingTx.utxoBundle, changeAddress, receiveAddress.address),
                pendingTx.feeAmount.toBigInteger(),
                pendingTx.amount.toBigInteger()
            ) to null
        }

    override fun doOnTransactionSuccess(pendingTx: PendingTx) {
        btcSource.incrementReceiveAddress()
        updateInternalBtcBalances(pendingTx)
    }

    override fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable) {
        Logger.e("BTC Send failed: $e")
        remoteLogger.logException(e)
    }

    // Update balance immediately after spend - until refresh from server
    private fun updateInternalBtcBalances(pendingTx: PendingTx) {
        try {
            val totalSent = pendingTx.totalSent.toBigInteger()
            val address = btcSource.xpubAddress
            btcDataManager.subtractAmountFromAddressBalance(
                address,
                totalSent.toLong()
            )
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .doOnComplete { btcSource.forceRefresh() }

    companion object {
        const val LARGE_TX_FIAT = "USD"
        const val LARGE_TX_FEE = 0.5
        const val LARGE_TX_SIZE = 1024
        val LARGE_TX_PERCENTAGE = BigDecimal(1.0)

        private val AVAILABLE_FEE_OPTIONS = setOf(FeeLevel.Regular, FeeLevel.Priority, FeeLevel.Custom)
        private val MAX_BTC_AMOUNT = 2_100_000_000_000_000L.toBigInteger()
    }

    // TEMP diagnostics - TODO Remove this once we're stable
    private val remoteLogger: RemoteLogger by inject()

    private fun Completable.logValidityFailure(): Completable =
        this.doOnError { remoteLogger.logException(it) }

    private fun fatalError(e: Throwable): Throwable {
        remoteLogger.logException(e)
        return e
    }
}

private val PendingTx.totalSent: Money
    get() = amount + feeAmount
