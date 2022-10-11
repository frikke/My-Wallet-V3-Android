package com.blockchain.coincore.impl.txEngine.walletconnect

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.eth.EthCryptoWalletAccount
import com.blockchain.coincore.eth.EthOnChainTxEngine
import com.blockchain.coincore.eth.EthSignMessage
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.coincore.eth.WalletConnectTarget
import com.blockchain.core.chains.ethereum.EthMessageSigner
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.storedatasource.FlushableDataSource
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.ethereum.util.EthUtils
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class WalletConnectSignEngine(
    private val assetEngine: EthOnChainTxEngine,
    private val ethMessageSigner: EthMessageSigner
) : TxEngine() {

    override val flushableDataSources: List<FlushableDataSource>
        get() = listOf()

    private val ethSignMessageTarget: EthereumSignMessageTarget
        get() = txTarget as EthereumSignMessageTarget

    override fun assertInputsValid() {
        check(txTarget is WalletConnectTarget)
        check(sourceAccount is EthCryptoWalletAccount)
        check(sourceAsset == CryptoCurrency.ETHER)
    }

    override fun start(
        sourceAccount: BlockchainAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRatesDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        super.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
        assetEngine.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        assetEngine.doInitialiseTx()
            .map { tx ->
                tx.copy(
                    amount = Money.zero(CryptoCurrency.ETHER),
                )
            }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> = Single.just(
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.WalletConnectHeader(
                    dAppLogo = ethSignMessageTarget.dAppLogoUrl,
                    dAppUrl = ethSignMessageTarget.dAppAddress,
                    dAppName = ethSignMessageTarget.label
                ),
                TxConfirmationValue.DAppInfo(
                    name = ethSignMessageTarget.label,
                    url = ethSignMessageTarget.dAppAddress
                ),
                TxConfirmationValue.Chain(
                    assetInfo = CryptoCurrency.ETHER
                ),
                TxConfirmationValue.SignEthMessage(
                    message = ethSignMessageTarget.message.readableData,
                    dAppName = ethSignMessageTarget.label
                )
            )
        )
    )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> = Single.just(pendingTx)

    override fun doUpdateFeeLevel(pendingTx: PendingTx, level: FeeLevel, customFeeAmount: Long): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> = Single.just(
        pendingTx
    )

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> = sourceAccount.receiveAddress.flatMap {
        if (it.address.equals(ethSignMessageTarget.message.address, true)) {
            Single.just(
                pendingTx.copy(
                    validationState = ValidationState.CAN_EXECUTE
                )
            )
        } else {
            Single.just(
                pendingTx.copy(
                    validationState = ValidationState.INVALID_ADDRESS
                )
            )
        }
    }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        when (ethSignMessageTarget.message.type) {
            EthSignMessage.SignType.PERSONAL_MESSAGE,
            EthSignMessage.SignType.MESSAGE -> ethMessageSigner.signEthMessage(ethSignMessageTarget.message.data)
            EthSignMessage.SignType.TYPED_MESSAGE -> ethMessageSigner.signEthTypedMessage(
                ethSignMessageTarget.message.data
            )
        }.map {
            TxResult.HashedTxResult(
                txId = EthUtils.decorateAndEncode(it), amount = Money.zero(CryptoCurrency.ETHER)
            )
        }

    override fun cancel(pendingTx: PendingTx): Completable {
        return ethSignMessageTarget.onTxCancelled()
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable {
        return ethSignMessageTarget.onTxCompleted(txResult)
    }
}
