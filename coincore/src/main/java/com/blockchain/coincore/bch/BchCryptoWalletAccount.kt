package com.blockchain.coincore.bch

import com.blockchain.coincore.AddressResolver
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxEngine
import com.blockchain.coincore.impl.AccountRefreshTrigger
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.chains.bitcoincash.BchBalanceCache
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.wallet.PubKeyStyle
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet.Companion.DEFAULT_ADDRESS_DESCRIPTOR
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.bch.CashAddress
import info.blockchain.wallet.coin.GenericMetadataAccount
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.bitcoinj.core.LegacyAddress

/*internal*/ class BchCryptoWalletAccount private constructor(
    private val payloadDataManager: PayloadDataManager,
    private val bchManager: BchDataManager,
    // Used to lookup the account in payloadDataManager to fetch receive address
    private val addressIndex: Int,
    override val exchangeRates: ExchangeRatesDataManager,
    private val feeDataManager: FeeDataManager,
    private val sendDataManager: SendDataManager,
    private val bchBalanceCache: BchBalanceCache,
    private var internalAccount: GenericMetadataAccount,
    private val walletPreferences: WalletStatusPrefs,
    private val refreshTrigger: AccountRefreshTrigger,
    override val addressResolver: AddressResolver,
) : CryptoNonCustodialAccount(
    CryptoCurrency.BCH
) {

    override val label: String
        get() = internalAccount.label

    override val isArchived: Boolean
        get() = internalAccount.isArchived

    override val isDefault: Boolean
        get() = addressIndex == bchManager.getDefaultAccountPosition()

    override val receiveAddress: Single<ReceiveAddress>
        get() = bchManager.getNextReceiveAddress(
            addressIndex
        ).map {
            val networkParams = BchMainNetParams.get()
            val address = LegacyAddress.fromBase58(networkParams, it)
            CashAddress.fromLegacyAddress(address)
        }.firstOrError()
            .map {
                BchAddress(address_ = it, label = label)
            }

    override suspend fun publicKey(): List<PublicKey> {
        return listOf(
            PublicKey(
                address = internalAccount.xpubs().default.address,
                style = PubKeyStyle.EXTENDED,
                descriptor = DEFAULT_ADDRESS_DESCRIPTOR
            )
        )
    }

    override fun getOnChainBalance(): Observable<Money> =
        Single.fromCallable { internalAccount.xpubs() }
            .flatMap { xpub -> bchManager.getBalance(xpub) }
            .map { Money.fromMinor(currency, it) }
            .toObservable()

    override val index: Int
        get() = addressIndex

    override val pubKeyDescriptor
        get() = BCH_PUBKEY_DESCRIPTOR

    override fun createTxEngine(target: TransactionTarget, action: AssetAction): TxEngine =
        BchOnChainTxEngine(
            feeManager = feeDataManager,
            sendDataManager = sendDataManager,
            bchDataManager = bchManager,
            payloadDataManager = payloadDataManager,
            requireSecondPassword = payloadDataManager.isDoubleEncrypted,
            walletPreferences = walletPreferences,
            bchBalanceCache = bchBalanceCache,
            resolvedAddress = addressResolver.getReceiveAddress(currency, target, action)
        )

    override fun updateLabel(newLabel: String): Completable {
        require(newLabel.isNotEmpty())
        val newAccount = internalAccount.updateLabel(newLabel)
        return bchManager.updateAccount(oldAccount = internalAccount, newAccount = newAccount).doOnComplete {
            internalAccount = newAccount
        }
    }

    override fun archive(): Completable =
        if (!isArchived && !isDefault) {
            toggleArchived()
        } else {
            Completable.error(IllegalStateException("${currency.networkTicker} Account $label cannot be archived"))
        }

    override fun unarchive(): Completable =
        if (isArchived) {
            toggleArchived()
        } else {
            Completable.error(IllegalStateException("${currency.networkTicker} Account $label cannot be unarchived"))
        }

    private fun toggleArchived(): Completable {
        val newAccount = internalAccount.updateArchivedState(!internalAccount.isArchived)
        return bchManager.updateAccount(oldAccount = internalAccount, newAccount = newAccount).doOnComplete {
            internalAccount = newAccount
        }
    }

    override fun setAsDefault(): Completable {
        require(!isDefault)
        return bchManager.updateDefaultAccount(internalAccount)
    }

    override val xpubAddress: String
        get() = internalAccount.xpubs().default.address

    override fun matches(other: CryptoAccount): Boolean =
        other is BchCryptoWalletAccount && other.xpubAddress == xpubAddress

    fun getReceiveAddressAtPosition(position: Int) =
        bchManager.getReceiveAddressAtPosition(addressIndex, position)

    internal fun forceRefresh() {
        refreshTrigger.forceAccountsRefresh()
    }

    override fun doesAddressBelongToWallet(address: String): Boolean =
        payloadDataManager.isOwnHDAddress(address)

    override val hasStaticAddress: Boolean = false

    companion object {
        fun createBchAccount(
            payloadManager: PayloadDataManager,
            jsonAccount: GenericMetadataAccount,
            bchManager: BchDataManager,
            addressIndex: Int,
            exchangeRates: ExchangeRatesDataManager,
            feeDataManager: FeeDataManager,
            sendDataManager: SendDataManager,
            bchBalanceCache: BchBalanceCache,
            walletPreferences: WalletStatusPrefs,
            refreshTrigger: AccountRefreshTrigger,
            addressResolver: AddressResolver,
        ) = BchCryptoWalletAccount(
            bchManager = bchManager,
            payloadDataManager = payloadManager,
            addressIndex = addressIndex,
            exchangeRates = exchangeRates,
            feeDataManager = feeDataManager,
            sendDataManager = sendDataManager,
            bchBalanceCache = bchBalanceCache,
            internalAccount = jsonAccount,
            walletPreferences = walletPreferences,
            refreshTrigger = refreshTrigger,
            addressResolver = addressResolver
        )

        const val BCH_PUBKEY_DESCRIPTOR = "p2pkh"
    }
}
