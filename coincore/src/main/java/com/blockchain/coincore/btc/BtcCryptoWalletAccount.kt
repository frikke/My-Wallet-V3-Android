package com.blockchain.coincore.btc

import com.blockchain.coincore.ActivitySummaryList
import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.AccountRefreshTrigger
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.transactionFetchCount
import com.blockchain.coincore.impl.transactionFetchOffset
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicBoolean
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList
import piuk.blockchain.androidcore.utils.extensions.then

/*internal*/ class BtcCryptoWalletAccount internal constructor(
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    // Used to lookup the account in payloadDataManager to fetch receive address
    private val hdAccountIndex: Int,
    override val exchangeRates: ExchangeRatesDataManager,
    private val internalAccount: JsonSerializableAccount,
    val isHDAccount: Boolean,
    private val walletPreferences: WalletStatusPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val refreshTrigger: AccountRefreshTrigger,
    override val addressResolver: AddressResolver,
) : CryptoNonCustodialAccount(
    CryptoCurrency.BTC
) {
    private val hasFunds = AtomicBoolean(false)

    override val label: String
        get() = internalAccount.label

    override val isArchived: Boolean
        get() = internalAccount.isArchived

    override val isDefault: Boolean
        get() = isHDAccount && payloadDataManager.defaultAccountIndex == hdAccountIndex

    override val isFunded: Boolean
        get() = hasFunds.get()

    override fun getOnChainBalance(): Observable<Money> =
        getAccountBalance(false)
            .toObservable()

    private fun getAccountBalance(forceRefresh: Boolean): Single<Money> =
        payloadDataManager.getAddressBalanceRefresh(xpubs, forceRefresh)
            .doOnSuccess {
                hasFunds.set(it.isPositive)
            }
            .map { it }

    override val receiveAddress: Single<ReceiveAddress>
        get() = when (internalAccount) {
            is Account -> {
                payloadDataManager.getNextReceiveAddress(
                    internalAccount
                ).singleOrError()
                    .map {
                        BtcAddress(address = it, label = label)
                    }
            }
            else -> Single.error(IllegalStateException("Cannot receive to Imported Account"))
        }

    override val activity: Single<ActivitySummaryList>
        get() = payloadDataManager.getAccountTransactions(
            xpubs,
            transactionFetchCount,
            transactionFetchOffset
        ).onErrorReturn { emptyList() }
            .mapList {
                BtcActivitySummaryItem(
                    it,
                    payloadDataManager,
                    exchangeRates,
                    this
                )
            }
            .flatMap {
                appendTradeActivity(custodialWalletManager, currency, it)
            }
            .doOnSuccess {
                setHasTransactions(it.isNotEmpty())
            }

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        BtcOnChainTxEngine(
            btcDataManager = payloadDataManager,
            sendDataManager = sendDataManager,
            feeManager = feeDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    override val stateAwareActions: Single<Set<StateAwareAction>>
        get() = super.stateAwareActions.map { actions ->
            if (!isHDAccount) {
                actions.toMutableSet().apply {
                    removeIf {
                        it.action == AssetAction.Receive
                    }
                }.toSet()
            } else actions
        }

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        return payloadDataManager.updateAccountLabel(internalAccount, newLabel)
    }

    override fun archive(): Completable {
        require(!isArchived)
        require(!isDefault)
        return toggleArchived()
    }

    override fun unarchive(): Completable {
        require(isArchived)
        return toggleArchived()
    }

    private fun toggleArchived(): Completable {
        val isArchived = this.isArchived

        return updateArchivedState(!isArchived)
            .then { payloadDataManager.updateAllTransactions() }
            .then { getAccountBalance(true).ignoreElement() }
            .doOnComplete { forceRefresh() }
    }

    private fun updateArchivedState(newIsArchived: Boolean): Completable {
        return payloadDataManager.updateAccountArchivedState(internalAccount, newIsArchived)
    }

    override fun setAsDefault(): Completable {
        require(!isDefault)
        require(isHDAccount)

        return payloadDataManager.setDefaultIndex(hdAccountIndex)
            .doOnComplete { forceRefresh() }
    }

    override val xpubAddress: String
        get() = when (internalAccount) {
            is Account -> internalAccount.xpubs.default.address
            is ImportedAddress -> internalAccount.address
            else -> throw java.lang.IllegalStateException("Unknown wallet type")
        }

    override val hasStaticAddress: Boolean = false

    val xpubs: XPubs
        get() = when (internalAccount) {
            is Account -> internalAccount.xpubs
            is ImportedAddress -> internalAccount.xpubs()
            else -> throw java.lang.IllegalStateException("Unknown wallet type")
        }

    fun getSigningKeys(utxo: SpendableUnspentOutputs, secondPassword: String): Single<List<SigningKey>> {
        if (isHDAccount) {
            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(secondPassword)
            }

            return Single.just(
                payloadDataManager.getHDKeysForSigning(
                    account = internalAccount as Account,
                    unspentOutputBundle = utxo
                )
            )
        } else {
            val password = if (payloadDataManager.isDoubleEncrypted) secondPassword else null
            return Single.just(
                listOf(
                    payloadDataManager.getAddressSigningKey(
                        importedAddress = internalAccount as ImportedAddress,
                        secondPassword = password
                    ) ?: throw IllegalStateException("Private key not found for legacy BTC address")
                )
            )
        }
    }

    fun getChangeAddress(): Single<String> {
        return if (isHDAccount) {
            payloadDataManager.getNextChangeAddress(internalAccount as Account)
                .singleOrError()
        } else {
            Single.just((internalAccount as ImportedAddress).address)
        }
    }

    fun incrementReceiveAddress() {
        if (isHDAccount) {
            val account = internalAccount as Account
            payloadDataManager.incrementChangeAddress(account)
            payloadDataManager.incrementReceiveAddress(account)
        }
    }

    fun getReceiveAddressAtPosition(position: Int): String? {
        require(isHDAccount)
        return payloadDataManager.getReceiveAddressAtPosition(internalAccount as Account, position)
    }

    override fun matches(other: CryptoAccount): Boolean =
        other is BtcCryptoWalletAccount && other.xpubAddress == xpubAddress

    internal fun forceRefresh() {
        refreshTrigger.forceAccountsRefresh()
    }

    override fun doesAddressBelongToWallet(address: String): Boolean =
        payloadDataManager.isOwnHDAddress(address)

    companion object {
        fun createHdAccount(
            jsonAccount: Account,
            payloadDataManager: PayloadDataManager,
            hdAccountIndex: Int,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            exchangeRates: ExchangeRatesDataManager,
            walletPreferences: WalletStatusPrefs,
            custodialWalletManager: CustodialWalletManager,
            refreshTrigger: AccountRefreshTrigger,
            addressResolver: AddressResolver,
        ) = BtcCryptoWalletAccount(
            payloadDataManager = payloadDataManager,
            hdAccountIndex = hdAccountIndex,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = jsonAccount,
            isHDAccount = true,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager,
            refreshTrigger = refreshTrigger,
            addressResolver = addressResolver
        )

        fun createImportedAccount(
            importedAccount: ImportedAddress,
            payloadDataManager: PayloadDataManager,
            sendDataManager: SendDataManager,
            feeDataManager: FeeDataManager,
            exchangeRates: ExchangeRatesDataManager,
            walletPreferences: WalletStatusPrefs,
            custodialWalletManager: CustodialWalletManager,
            refreshTrigger: AccountRefreshTrigger,
            addressResolver: AddressResolver,
        ) = BtcCryptoWalletAccount(
            payloadDataManager = payloadDataManager,
            hdAccountIndex = IMPORTED_ACCOUNT_NO_INDEX,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = importedAccount,
            isHDAccount = false,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager,
            refreshTrigger = refreshTrigger,
            addressResolver = addressResolver
        )

        private const val IMPORTED_ACCOUNT_NO_INDEX = -1
    }
}
