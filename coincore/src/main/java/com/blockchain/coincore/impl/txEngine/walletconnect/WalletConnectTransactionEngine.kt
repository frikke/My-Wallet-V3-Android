package com.blockchain.coincore.impl.txEngine.walletconnect

import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.eth.EthCryptoWalletAccount
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.coincore.eth.WalletConnectTarget
import com.blockchain.coincore.evm.L1EvmNonCustodialAccount
import com.blockchain.coincore.toUserFiat
import com.blockchain.coincore.updateTxValidity
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.then
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.NetworkType
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.util.EthUtils
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.utils.Convert

class WalletConnectTransactionEngine(
    private val feeManager: FeeDataManager,
    private val ethDataManager: EthDataManager
) : TxEngine() {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    override fun ensureSourceBalanceFreshness() {}

    private val ethSignMessageTarget: EthereumSendTransactionTarget
        get() = txTarget as EthereumSendTransactionTarget

    override fun assertInputsValid() {
        check(txTarget is WalletConnectTarget)
        check(txTarget is EthereumSendTransactionTarget)
        check(sourceAccount is EthCryptoWalletAccount || sourceAccount is L1EvmNonCustodialAccount)
        check(sourceAsset is CryptoCurrency.ETHER || (sourceAsset as AssetInfo).coinNetwork?.type == NetworkType.EVM)
    }

    override fun doInitialiseTx(): Single<PendingTx> {
        return Single.just(
            PendingTx(
                amount = ethSignMessageTarget.amount,
                totalBalance = Money.zero(sourceAsset),
                availableBalance = Money.zero(sourceAsset),
                feeForFullAvailable = Money.zero(sourceAsset),
                feeAmount = Money.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = setOf(FeeLevel.Regular),
                    asset = sourceAsset as AssetInfo
                ),
                selectedFiat = userFiat
            )
        )
    }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.zip(
            sourceAccount.balanceRx().firstOrError(),
            absoluteFees()
        ) { balance, fees ->
            pendingTx.copy(
                availableBalance = balance.withdrawable - fees,
                feeForFullAvailable = fees,
                feeAmount = fees,
                txConfirmations = listOfNotNull(
                    TxConfirmationValue.WalletConnectHeader(
                        dAppLogo = ethSignMessageTarget.dAppLogoURL,
                        dAppUrl = ethSignMessageTarget.dAppAddress,
                        dAppName = ethSignMessageTarget.label
                    ),
                    TxConfirmationValue.From(sourceAccount, sourceAsset),
                    TxConfirmationValue.ToWithNameAndAddress(
                        label = ethSignMessageTarget.label,
                        address = ethSignMessageTarget.address
                    ),
                    TxConfirmationValue.Amount(
                        amount = pendingTx.amount,
                        isImportant = false
                    ),
                    TxConfirmationValue.CompoundNetworkFee(
                        sendingFeeInfo = if (!fees.isZero) {
                            FeeInfo(
                                feeAmount = fees,
                                fiatAmount = fees.toUserFiat(exchangeRates),
                                asset = sourceAsset
                            )
                        } else {
                            null
                        },
                        feeLevel = pendingTx.feeSelection.selectedLevel
                    ),
                    TxConfirmationValue.Total(
                        totalWithFee = pendingTx.amount.plus(
                            fees
                        ),
                        exchange = pendingTx.amount.toUserFiat(exchangeRates)
                            .plus(fees.toUserFiat(exchangeRates))
                    )
                )
            )
        }

    private fun absoluteFees(): Single<Money> =
        if (sourceAsset is CryptoCurrency.ETHER) {
            gasLimit().zipWith(gasPrice()).map { (gasLimit, gasPrice) ->
                Money.fromMinor(
                    sourceAsset,
                    gasLimit * gasPrice.toBigInteger()
                )
            }
        } else {
            feeOptions().map { feeOptions ->
                val gasLimitContract = feeOptions.gasLimitContract
                getValueForFeeLevel(gasLimitContract, feeOptions.regularFee, (sourceAsset as AssetInfo))
            }
        }

    private fun feeOptions(): Single<FeeOptions> =
        feeManager.getErc20FeeOptions(
            (sourceAsset as AssetInfo).coinNetwork!!.networkTicker,
            (sourceAsset as AssetInfo).l2identifier
        )
            .singleOrError()

    private fun getValueForFeeLevel(gasLimitContract: Long, feeLevel: Long, assetInfo: AssetInfo) =
        CryptoValue.fromMinor(
            assetInfo,
            Convert.toWei(
                BigDecimal.valueOf(gasLimitContract * feeLevel),
                Convert.Unit.GWEI
            )
        )

    private fun gasLimit(): Single<BigInteger> {
        return ethSignMessageTarget.gasLimit?.let { gas ->
            Single.just(gas)
        } ?: feeManager.ethFeeOptions.firstOrError().map {
            it.gasLimit.toBigInteger()
        }
    }

    private fun gasPrice(): Single<Money> {
        return ethSignMessageTarget.gasPrice?.let { gas ->
            Single.just(Money.fromMinor(sourceAsset, gas))
        } ?: feeManager.ethFeeOptions.firstOrError().map {
            Money.fromMinor(
                sourceAsset,
                Convert.toWei(
                    BigDecimal.valueOf(it.regularFee),
                    Convert.Unit.GWEI
                ).toBigInteger()
            )
        }
    }

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> = Single.just(pendingTx)

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Single.zip(
            sourceAccount.balanceRx().map {
                it.withdrawable
            }.firstOrError(),
            absoluteFees()
        ) { balance: Money, fee ->
            if (fee + pendingTx.amount > balance) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            } else {
                true
            }
        }.ignoreElement()

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.amount < Money.zero(sourceAsset)) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> {
        return validateAmounts(pendingTx)
            .then { validateSourceAddress() }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateNoPendingTx() }
            .updateTxValidity(pendingTx)
    }

    private fun validateNoPendingTx() = ethDataManager.isLastTxPending()
        .flatMapCompletable { hasUnconfirmed: Boolean ->
            if (hasUnconfirmed) {
                Completable.error(TxValidationFailure(ValidationState.HAS_TX_IN_FLIGHT))
            } else {
                Completable.complete()
            }
        }

    private fun validateSourceAddress() = sourceAccount.receiveAddress.flatMapCompletable {
        if (it.address.equals(ethSignMessageTarget.transactionSource, true)) {
            Completable.complete()
        } else {
            Completable.error(TxValidationFailure(ValidationState.INVALID_ADDRESS))
        }
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> {
        val nonce = ethSignMessageTarget.nonce?.let {
            Single.just(it)
        } ?: ethDataManager.getNonce()

        return Singles.zip(nonce, gasPrice(), gasLimit()).map { (nonce, gasPrice, gasLimit) ->
            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = ethSignMessageTarget.address,
                gasPriceWei = gasPrice.toBigInteger(),
                gasLimitGwei = gasLimit,
                data = ethSignMessageTarget.data,
                weiValue = pendingTx.amount.toBigInteger()
            )
        }.flatMap {
            ethDataManager.signEthTransaction(it, secondPassword)
        }.flatMap { signed ->
            when (ethSignMessageTarget.method) {
                EthereumSendTransactionTarget.Method.SEND -> ethDataManager.pushTx(signed)
                    .map { txHash ->
                        TxResult.HashedTxResult(txHash, pendingTx.amount)
                    }
                EthereumSendTransactionTarget.Method.SIGN ->
                    Single.just(
                        TxResult.HashedTxResult(
                            txId = EthUtils.decorateAndEncode(signed),
                            amount = pendingTx.amount
                        )
                    )
            }
        }
    }

    override fun cancel(pendingTx: PendingTx): Completable {
        return ethSignMessageTarget.onTxCancelled()
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable {
        return ethSignMessageTarget.onTxCompleted(txResult)
    }
}
