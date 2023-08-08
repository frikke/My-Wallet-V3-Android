package com.blockchain.sunriver

import com.blockchain.fees.FeeType
import com.blockchain.logging.CustomEventBuilder
import com.blockchain.logging.EventLogger
import com.blockchain.logging.LastTxUpdater
import com.blockchain.sunriver.datamanager.XlmAccount
import com.blockchain.sunriver.datamanager.XlmMetaData
import com.blockchain.sunriver.datamanager.XlmMetaDataInitializer
import com.blockchain.sunriver.datamanager.default
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.utils.toHex
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.LabeledAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.IllegalStateException
import org.stellar.sdk.KeyPair

data class XlmAccountReference(
    override val label: String,
    /**
     * address
     */
    val accountId: String,
    /**
     * pubkey
     */
    val pubKey: String?
) : LabeledAccount

data class BalanceAndMin(
    val balance: Money,
    val minimumBalance: Money
)

class XlmDataManager internal constructor(
    private val horizonProxy: HorizonProxy,
    private val metaDataInitializer: XlmMetaDataInitializer,
    private val xlmSecretAccess: XlmSecretAccess,
    private val memoMapper: MemoMapper,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val xlmTimeoutFetcher: XlmTransactionTimeoutFetcher,
    private val lastTxUpdater: LastTxUpdater,
    private val eventLogger: EventLogger,
    xlmHorizonUrlFetcher: XlmHorizonUrlFetcher,
    xlmHorizonDefUrl: String
) {
    val publicKey: Single<String>
        get() = defaultAccount().map { account ->
            account.pubKey ?: throw IllegalStateException("Missing public key")
        }.ensureUrlUpdated()

    private val xlmProxyUrl = xlmHorizonUrlFetcher
        .xlmHorizonUrl(xlmHorizonDefUrl)
        .doOnSuccess {
            horizonProxy.update(it)
        }.cache()

    fun sendFunds(
        sendDetails: SendDetails,
        secondPassword: String? = null
    ): Single<SendFundsResult> =
        Single.defer {
            Singles.zip(
                xlmSecretAccess.getPrivate(
                    HorizonKeyPair.Public(sendDetails.fromXlm.accountId),
                    secondPassword
                ),
                xlmTimeoutFetcher.transactionTimeout(),
                xlmProxyUrl
            ).map {
                horizonProxy.sendTransaction(
                    it.first.toKeyPair(),
                    sendDetails.toAddress,
                    sendDetails.value,
                    memoMapper.mapMemo(sendDetails.memo),
                    it.second,
                    sendDetails.fee
                )
            }.map { it.mapToSendFundsResult(sendDetails) }
                .flatMap {
                    if (it.success) {
                        val event = sendDetails.memo?.let { memo ->
                            memoToEvent(memo)
                        } ?: noMemoEvent

                        eventLogger.logEvent(event)

                        lastTxUpdater.updateLastTxTime().onErrorComplete().toSingleDefault(it)
                    } else {
                        Single.just(it)
                    }
                }
        }.ensureUrlUpdated()

    fun dryRunSendFunds(
        sendDetails: SendDetails
    ): Single<SendFundsResult> =
        Single.defer {
            horizonProxy.dryRunTransaction(
                HorizonKeyPair.Public(sendDetails.fromXlm.accountId).toKeyPair(),
                sendDetails.toAddress,
                sendDetails.value,
                memoMapper.mapMemo(sendDetails.memo),
                sendDetails.fee
            ).mapToSendFundsResult(sendDetails).just().ensureUrlUpdated()
        }.ensureUrlUpdated()

    fun isAddressValid(address: String): Boolean =
        try {
            KeyPair.fromAccountId(address)
            true
        } catch (e: Exception) {
            false
        }

    private fun <T> T.just(): Single<T> = Single.just(this)

    private val wallet = Single.defer { metaDataInitializer.initWalletMaybePrompt.toSingle() }
    private val maybeWallet = Maybe.defer { metaDataInitializer.initWalletMaybe }

    fun getBalance(accountReference: XlmAccountReference): Single<CryptoValue> =
        getBalance(accountReference.accountId)

    private fun getBalance(address: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getBalance(address) }.ensureUrlUpdated()
            .subscribeOn(Schedulers.io())

    private fun getBalanceAndMin(accountReference: XlmAccountReference): Single<BalanceAndMin> =
        Single.fromCallable { horizonProxy.getBalanceAndMin(accountReference.accountId) }.ensureUrlUpdated()
            .subscribeOn(Schedulers.io())

    fun getBalance(): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount().flatMap { getBalance(it).toMaybe() },
            Maybe.just(CryptoValue.zero(CryptoCurrency.XLM))
        ).firstOrError()

    /**
     * Balance - minimum - fees
     */
    fun getMaxSpendableAfterFees(feeType: FeeType): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount()
                .flatMapSingle { accountRef ->
                    xlmFeesFetcher.operationFee(feeType).map { accountRef to it }
                }
                .flatMap { (accountRef, fee) ->
                    getBalanceAndMin(accountRef).map {
                        (it.balance - it.minimumBalance - fee) as CryptoValue
                    }.toMaybe()
                },
            Maybe.just(CryptoValue.zero(CryptoCurrency.XLM))
        ).firstOrError()

    fun getBalanceAndMin(): Single<BalanceAndMin> =
        Maybe.concat(
            maybeDefaultAccount().flatMap {
                getBalanceAndMin(it).toMaybe()
            },
            Maybe.just(
                BalanceAndMin(
                    CryptoValue.zero(CryptoCurrency.XLM),
                    CryptoValue.zero(CryptoCurrency.XLM)
                )
            )
        ).firstOrError()

    fun defaultAccount(): Single<XlmAccountReference> =
        defaultXlmAccount()
            .map(XlmAccount::toReference)

    fun defaultAccountReference(): Single<XlmAccountReference> = defaultAccount()

    fun maybeDefaultAccount(): Maybe<XlmAccountReference> =
        maybeDefaultXlmAccount().map(XlmAccount::toReference)

    fun getTransactionList(accountReference: XlmAccountReference): Single<List<XlmTransaction>> =
        Single.fromCallable {
            horizonProxy.getTransactionList(accountReference.accountId)
                .map(accountReference.accountId, horizonProxy)
        }.ensureUrlUpdated()

    fun getTransactionList(): Single<List<XlmTransaction>> =
        defaultAccount().flatMap { getTransactionList(it) }

    private fun defaultXlmAccount() =
        wallet.map(XlmMetaData::default)

    private fun maybeDefaultXlmAccount() =
        maybeWallet.map(XlmMetaData::default)

    private fun <T : Any> Single<T>.ensureUrlUpdated(): Single<T> =
        xlmProxyUrl.flatMap {
            this
        }

    fun memoToEvent(memo: Memo): CustomEventBuilder = if (memo.isEmpty()) {
        noMemoEvent
    } else {
        MemoTypeLog().putMemoType(memo.type!!)
    }

    fun updateAccountLabel(newLabel: String): Completable =
        metaDataInitializer.updateAccountLabel(newLabel)

    private val noMemoEvent = object : CustomEventBuilder("Memo not Used") {}

    private class MemoTypeLog : CustomEventBuilder("Memo Used") {

        fun putMemoType(type: String): MemoTypeLog {
            putCustomAttribute("Type", type)
            return this
        }
    }
}

internal fun HorizonProxy.SendResult.mapToSendFundsResult(sendDetails: SendDetails): SendFundsResult =
    if (success) {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = 0,
            confirmationDetails = SendConfirmationDetails(
                sendDetails = sendDetails,
                fees = CryptoValue.fromMinor(CryptoCurrency.XLM, transaction!!.fee.toBigInteger())
            ),
            hash = transaction.hash().toHex()
        )
    } else {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = failureReason.errorCode,
            errorValue = failureValue,
            confirmationDetails = null,
            hash = null,
            errorExtra = failureExtra
        )
    }

private val SendDetails.fromXlm
    get() = from as? XlmAccountReference
        ?: throw XlmSendException("Source account reference is not an Xlm reference")

class XlmSendException(message: String) : RuntimeException(message)

private fun XlmAccount.toReference() =
    XlmAccountReference(label = label ?: "", accountId = publicKey, pubKey = pubKey)

class SendException(
    result: SendFundsResult
) : RuntimeException("SendException - code: ${result.errorCode}, extra: '${result.errorExtra}'") {
    val errorCode = result.errorCode
    val hash = result.hash
    val details = result.sendDetails
}

data class SendDetails(
    val from: XlmAccountReference,
    val value: CryptoValue,
    val toAddress: String,
    val toLabel: String = "",
    val fee: CryptoValue,
    val memo: Memo? = null
) {
    constructor(
        from: XlmAccountReference,
        value: CryptoValue,
        toAddress: String,
        fee: CryptoValue,
        memo: Memo? = null
    ) : this(from, value, toAddress, "", fee, memo)
}

data class Memo(

    val value: String,

    /**
     * This is open type for TransactionSender to interpret however it likes.
     * For example, the types of memo available to Xlm are different to those available in other currencies.
     */
    val type: String? = null
) {
    fun isEmpty() = value.isBlank()

    companion object {
        val None = Memo("", null)
        const val MEMO_TYPE_TEXT = "text"
        const val MEMO_TYPE_ID = "id"
    }
}

data class SendFundsResult(
    val sendDetails: SendDetails,
    /**
     * Currency Specific error code, refer to the implementation
     */
    val errorCode: Int,
    val confirmationDetails: SendConfirmationDetails?,
    val hash: String?,
    val errorValue: CryptoValue? = null,
    val errorExtra: String? = null
) {
    val txHash: String
        get() = hash ?: throw SendException(this)
    val success = errorCode == 0 && hash != null
}

data class SendConfirmationDetails(
    val sendDetails: SendDetails,
    val fees: CryptoValue
) {
    val from: XlmAccountReference = sendDetails.from
    val to: String = sendDetails.toAddress
    val amount: CryptoValue = sendDetails.value
}
