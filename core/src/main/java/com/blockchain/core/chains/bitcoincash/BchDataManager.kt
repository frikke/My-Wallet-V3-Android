package com.blockchain.core.chains.bitcoincash

import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.core.utils.schedulers.applySchedulers
import com.blockchain.logging.Logger
import com.blockchain.logging.RemoteLogger
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.utils.then
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.bch.CashAddress
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.coin.GenericMetadataWallet
import info.blockchain.wallet.crypto.DeterministicAccount
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.Derivation
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.math.BigInteger
import org.bitcoinj.core.LegacyAddress

class BchDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val bchDataStore: BchDataStore,
    private val bitcoinApi: NonCustodialBitcoinService,
    private val defaultLabels: DefaultLabels,
    private val bchBalanceCache: BchBalanceCache,
    private val metadataRepository: MetadataRepository,
    private val remoteLogger: RemoteLogger
) {

    /**
     * Clears the currently stored BCH wallet from memory.
     */
    fun clearAccountDetails() {
        bchDataStore.clearData()
    }

    /**
     * Fetches BCH Wallet stored in metadata. If metadata entry doesn't exists it will be created.
     *
     * @param defaultLabel The ETH address default label to be used if metadata entry doesn't exist
     * @return An [Completable]
     */
    fun initBchWallet(defaultLabel: String): Completable =
        fetchMetadata(defaultLabel, payloadDataManager.accounts.size).map { wallet ->
            MetadataPair(
                wallet,
                false
            )
        }
            .defaultIfEmpty(
                MetadataPair(
                    createMetadata(defaultLabel, payloadDataManager.accounts.size),
                    true
                )
            )
            .doOnSuccess { (metadata, _) ->
                bchDataStore.bchMetadata = restoreBchWallet(metadata)
            }
            .flatMapCompletable { metadataPair ->
                val saveToMetadataCompletable = if (metadataPair.needsSave) {
                    metadataRepository.saveRawValue(
                        bchDataStore.bchMetadata!!.toJson(),
                        MetadataEntry.METADATA_BCH
                    )
                } else {
                    Completable.complete()
                }
                saveToMetadataCompletable.then {
                    correctBtcOffsetIfNeed()
                }
            }
            .subscribeOn(Schedulers.io())

    fun updateAccount(oldAccount: GenericMetadataAccount, newAccount: GenericMetadataAccount): Completable {
        val payload = bchDataStore.bchMetadata!!.updateAccount(oldAccount = oldAccount, newAccount = newAccount)
        return updateBchPayload(payload)
    }

    fun updateAccountsLabel(updatedAccounts: Map<GenericMetadataAccount, String>): Completable {
        val payload = bchDataStore.bchMetadata!!.updatedAccounts(updatedAccounts)
        return updateBchPayload(payload)
    }

    fun updateDefaultAccount(internalAccount: GenericMetadataAccount): Completable {
        val newIndex = bchDataStore.bchMetadata!!.accounts.indexOf(internalAccount)
        val payload = bchDataStore.bchMetadata!!.updateDefaultIndex(newIndex)
        return updateBchPayload(payload)
    }

    fun getXpubForIndex(index: Int): String {
        remoteLogger.logState("Missing bch xpub for index", index.toString())
        val account = payloadDataManager.accounts.getOrNull(index)
        if (account == null) {
            remoteLogger.logState("Request payload index is null", index.toString())
            throw IllegalStateException("Account not found")
        }
        if (account.xpubForDerivation(Derivation.LEGACY_TYPE) == null) {
            remoteLogger.logState("Legacy xpub for index not found", index.toString())
            throw IllegalStateException("Account not found")
        }
        return account.xpubForDerivation(Derivation.LEGACY_TYPE)!!
    }

    private fun updateBchPayload(payload: GenericMetadataWallet): Completable = metadataRepository.saveRawValue(
        payload.toJson(),
        MetadataEntry.METADATA_BCH
    ).doOnComplete {
        bchDataStore.bchMetadata = payload
    }

    fun updateTransactions(): Completable =
        Completable.fromObservable(getWalletTransactions(50, 50))

    // VisibleForTesting
    internal fun fetchMetadata(
        defaultLabel: String,
        accountTotal: Int
    ): Maybe<GenericMetadataWallet> =
        metadataRepository.loadRawValue(MetadataEntry.METADATA_BCH)
            .map { walletJson ->
                // Fetch wallet
                val metaData = GenericMetadataWallet.fromJson(walletJson)
                // Sanity check (Add missing metadata accounts)
                val missingBchAccounts = getAccountsAfterIndex(defaultLabel, metaData.accounts.size, accountTotal)
                bchDataStore.bchMetadata = metaData.copy(accounts = metaData.accounts.plus(missingBchAccounts))
                metaData
            }

    // VisibleForTesting
    internal fun createMetadata(defaultLabel: String, accountTotal: Int): GenericMetadataWallet {
        val bchAccounts = getAccountsAfterIndex(defaultLabel, 0, accountTotal)

        return GenericMetadataWallet(
            _defaultAcccountIdx = 0,
            accounts = bchAccounts,
            _hasSeen = true
        )
    }

    private fun getAccountsAfterIndex(
        defaultLabel: String,
        startingAccountIndex: Int,
        accountTotal: Int
    ): List<GenericMetadataAccount> {
        val bchAccounts = arrayListOf<GenericMetadataAccount>()
        ((startingAccountIndex + 1)..accountTotal)
            .map {
                return@map when (it) {
                    in 2..accountTotal -> "$defaultLabel $it"
                    else -> defaultLabel
                }
            }
            .forEach { bchAccounts.add(GenericMetadataAccount(it, false)) }

        return bchAccounts
    }

    /**
     * Restore bitcoin cash wallet
     */
    // VisibleForTesting
    internal fun restoreBchWallet(walletMetadata: GenericMetadataWallet): GenericMetadataWallet {
        if (!payloadDataManager.isDoubleEncrypted) {
            bchDataStore.bchWallet = BitcoinCashWallet.restore(
                bitcoinApi,
                BitcoinCashWallet.BITCOIN_COIN_PATH,
                payloadDataManager.mnemonic,
                ""
            )

            // BCH Metadata does not store xpub - get from btc wallet since PATH is the same
            return walletMetadata.copy(
                accounts = payloadDataManager.accounts.mapIndexed { i, account ->
                    bchDataStore.bchWallet?.addAccount()
                    val xpub = account.xpubForDerivation(Derivation.LEGACY_TYPE)
                    checkXpubAndLog(xpub, "restorebchwallet_update", i)
                    walletMetadata.accounts[i].updateXpub(xpub!!)
                }
            )
        } else {
            val params = BchMainNetParams.get()
            bchDataStore.bchWallet = BitcoinCashWallet.createWatchOnly(
                bitcoinApi,
                params
            )

            // NB! A watch-only account xpub != account xpub, they do however derive the same addresses.
            // Only use this [DeterministicAccount] to derive receive/change addresses. Don't use xpub as multiaddr etc parameter.

            return walletMetadata.copy(
                accounts = payloadDataManager.accounts.mapIndexed { i, account ->
                    bchDataStore.bchWallet?.addWatchOnlyAccount(account.xpubForDerivation(Derivation.LEGACY_TYPE))
                    val xpub = account.xpubForDerivation(Derivation.LEGACY_TYPE)
                    checkXpubAndLog(xpub, "restorebchwallet_update", i)
                    walletMetadata.accounts[i].updateXpub(xpub!!)
                }
            )
        }
    }

    /**
     * Create more btc accounts to catch up to BCH stored in metadata if required.
     *
     * BCH metadata might have more accounts than a restored BTC wallet. When a BTC wallet is restored
     * from mnemonic we will only look ahead 5 accounts to see if the account contains any transactions.
     *
     * @return Boolean value to indicate if bitcoin wallet payload needs to sync to the server
     */
    fun correctBtcOffsetIfNeed(): Completable {
        val startingAccountIndex = payloadDataManager.accounts.size
        val bchAccountSize = bchDataStore.bchMetadata?.accounts?.size ?: 0
        val difference = bchAccountSize.minus(startingAccountIndex)

        return if (difference > 0) {
            val singles = (startingAccountIndex until bchAccountSize)
                .map {
                    val accountNumber = it + 1
                    val label = defaultLabels.getDefaultNonCustodialWalletLabel()
                    val newAccountLabel = "$label $accountNumber"
                    payloadDataManager.addAccountWithLabel(newAccountLabel).doOnSuccess { account ->
                        bchDataStore.bchMetadata = bchDataStore.bchMetadata!!.updateXpubForAccountIndex(
                            it,
                            account.xpubForDerivation(Derivation.LEGACY_TYPE)!!
                        )
                    }
                }
            Single.merge(singles).ignoreElements()
        } else Completable.complete()
    }

    /**
     * Restore bitcoin cash wallet from mnemonic.
     */
    fun decryptWatchOnlyWallet(mnemonic: List<String>) {
        bchDataStore.bchWallet = BitcoinCashWallet.restore(
            bitcoinApi,
            BitcoinCashWallet.BITCOIN_COIN_PATH,
            mnemonic,
            ""
        )

        val accounts = payloadDataManager.accounts.mapIndexed { i, account ->
            bchDataStore.bchWallet?.addAccount()
            val xpub = account.xpubForDerivation(Derivation.LEGACY_TYPE)
            checkXpubAndLog(xpub, "decryptwatchonly", i)
            bchDataStore.bchMetadata!!.accounts[i].copy(_xpub = xpub)
        }
        bchDataStore.bchMetadata = bchDataStore.bchMetadata!!.copy(accounts = accounts)
    }

    /**
     * Adds a [GenericMetadataAccount] to the BCH wallet. The wallet will have to be saved at this
     * point. This assumes that a new [info.blockchain.wallet.payload.data.Account] has already
     * been added to the user's Payload, otherwise xPubs could get out of sync.
     */
    fun createAccount(bitcoinXpub: String): Completable {
        if (bchDataStore.bchWallet!!.isWatchOnly) {
            bchDataStore.bchWallet!!.addWatchOnlyAccount(bitcoinXpub)
        } else {
            bchDataStore.bchWallet!!.addAccount()
        }

        val defaultLabel = defaultLabels.getDefaultNonCustodialWalletLabel()
        val count = bchDataStore.bchWallet!!.accountTotal
        val payload = bchDataStore.bchMetadata!!.addAccount(
            GenericMetadataAccount(
                label = """$defaultLabel $count""",
                _archived = false,
                _xpub = bitcoinXpub
            )
        )
        return updateBchPayload(payload)
    }

    private fun getActiveXpubs(): List<XPubs> {
        val accounts = bchDataStore.bchMetadata?.accounts
        return accounts?.let {
            it.filterNot { a -> a.isArchived }
                .map { a -> a.xpubs() }
        } ?: emptyList()
    }

    fun getImportedAddressStringList(): List<String> = payloadDataManager.importedAddressStringList

    fun getAddressBalance(address: String): CryptoValue =
        CryptoValue(CryptoCurrency.BCH, bchDataStore.bchBalances[address] ?: BigInteger.ZERO)

    private fun updateBalanceForAddress(address: String, balance: BigInteger) {
        bchDataStore.bchBalances[address] = balance
    }

    fun getBalance(xpubs: XPubs): Single<BigInteger> =
        bchBalanceCache.getBalanceOfAddresses(getActiveXpubs()).map {
            it[xpubs.default.address]?.finalBalance ?: throw IllegalStateException("Balance call error")
        }.doOnSuccess { balance ->
            updateBalanceForAddress(xpubs.default.address, balance)
        }.doOnError(Logger::e)

    fun getAddressTransactions(
        address: String,
        limit: Int,
        offset: Int
    ): Observable<List<TransactionSummary>> =
        Observable.fromCallable { fetchAddressTransactions(address, limit, offset) }.applySchedulers()

    fun getWalletTransactions(limit: Int = 50, offset: Int = 0): Observable<List<TransactionSummary>> =
        Observable.fromCallable { fetchWalletTransactions(limit, offset) }.applySchedulers()

    fun getAccountMetadataList(): List<GenericMetadataAccount> =
        bchDataStore.bchMetadata?.accounts ?: emptyList()

    fun getAccountList(): List<DeterministicAccount> = bchDataStore.bchWallet!!.accounts

    fun getDefaultAccountPosition(): Int = bchDataStore.bchMetadata?.defaultAcccountIdx ?: 0

    /**
     * Allows you to generate a BCH receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [GenericMetadataAccount] you wish to generate an address from
     * @param addressIndex Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getReceiveAddressAtPosition(accountIndex: Int, addressIndex: Int): String? =
        bchDataStore.bchWallet?.getReceiveAddressAtPosition(accountIndex, addressIndex)

    fun getNextReceiveAddress(accountIndex: Int): Observable<String> = Observable.fromCallable {
        bchDataStore.bchWallet!!.getNextReceiveAddress(accountIndex)
    }

    /**
     * Generates a Base58 Bitcoin Cash receive address for an account at a given position and then formats this address
     * to CashAddress
     * Example: 14yYiZ5kzWhzSr6UKbWe6AKi46SfxvYneb --> bitcoincash:qq4e5fv3mdapcmhr9rm290ywzeu94288svt60rh64g
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in Base58 format
     */
    fun getNextCashReceiveAddress(accountIndex: Int): Observable<String> =
        Observable.fromCallable {
            bchDataStore.bchWallet!!.getNextReceiveAddress(accountIndex)
        }.map {
            val params = BchMainNetParams.get()
            val address = LegacyAddress.fromBase58(params, it)
            CashAddress.fromLegacyAddress(address)
        }

    /**
     * Generates a bech32 Bitcoin Cash receive address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash receive address in bech32 format
     */
    fun getNextReceiveCashAddress(accountIndex: Int): Observable<String> =
        Observable.fromCallable {
            bchDataStore.bchWallet!!.getNextReceiveCashAddress(accountIndex)
        }

    /**
     * Generates a Base58 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getNextChangeAddress(accountIndex: Int): Observable<String> =
        Observable.fromCallable {
            bchDataStore.bchWallet!!.getNextChangeAddress(accountIndex)
        }

    /**
     * Generates a bech32 Bitcoin Cash change address for an account at a given position. The
     * address returned will be the next unused in the chain.
     *
     * @param accountIndex The index of the [DeterministicAccount] you wish to generate an address from
     * @return A Bitcoin Cash change address in bech32 format
     */
    fun getNextChangeCashAddress(accountIndex: Int): Observable<String> =
        Observable.fromCallable {
            bchDataStore.bchWallet!!.getNextChangeCashAddress(accountIndex)
        }

    /**
     * Allows you to generate a BCH change address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param accountIndex The index of the [Account] you wish to generate an address from
     * @param addressIndex Represents how many positions on the chain beyond what is already used that
     * you wish to generate
     * @return A Bitcoin Cash change address in Base58 format
     */
    fun getChangeAddressAtPosition(accountIndex: Int, addressIndex: Int): Observable<String> =
        Observable.fromCallable {
            bchDataStore.bchWallet!!.getChangeAddressAtPosition(accountIndex, addressIndex)
        }

    fun incrementNextReceiveAddress(xpub: String): Completable =
        Completable.fromCallable {
            bchDataStore.bchWallet!!.incrementNextReceiveAddress(xpub)
        }

    fun incrementNextChangeAddress(xpub: String): Completable =
        Completable.fromCallable {
            bchDataStore.bchWallet!!.incrementNextChangeAddress(xpub)
        }

    fun isOwnAddress(address: String) = bchDataStore.bchWallet?.isOwnAddress(address) ?: false

    /**
     * Converts any Bitcoin Cash address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as imported address.
     * @return Account or imported address label
     */
    fun getLabelFromBchAddress(address: String): String? {
        val xpub = bchDataStore.bchWallet?.getXpubFromAddress(address)

        return bchDataStore.bchMetadata?.accounts?.find { it.xpubs().default.address == xpub }?.label
    }

    // /////////////////////////////////////////////////////////////////////////
    // Web requests that require wrapping in Observables
    // /////////////////////////////////////////////////////////////////////////

    private fun fetchAddressTransactions(
        address: String,
        limit: Int,
        offset: Int
    ): List<TransactionSummary> =
        bchDataStore.bchWallet!!.getTransactions(
            getActiveXpubs(),
            listOf(address),
            limit,
            offset
        )

    private fun fetchWalletTransactions(
        limit: Int,
        offset: Int
    ): List<TransactionSummary> =
        bchDataStore.bchWallet!!.getTransactions(
            getActiveXpubs(),
            null,
            limit,
            offset
        )

    fun getXpubFromAddress(address: String) =
        bchDataStore.bchWallet?.getXpubFromAddress(address)

    fun getHDKeysForSigning(
        account: DeterministicAccount,
        unspentOutputs: List<Utxo>
    ) = bchDataStore.bchWallet!!.getHDKeysForSigning(account, unspentOutputs)

    fun subtractAmountFromAddressBalance(account: String, amount: BigInteger) =
        bchDataStore.bchWallet!!.subtractAmountFromAddressBalance(account, amount)

    private data class MetadataPair(val metadata: GenericMetadataWallet, val needsSave: Boolean)

    private fun checkXpubAndLog(xpub: String?, callSite: String, accountIndex: Int) {
        if (xpub == null) {
            // We should not have a null xpub. Something is very wrong; let's write some
            // info to the remote logger and see if that gives a clue
            remoteLogger.logState("xpub_$callSite", "hit")
            remoteLogger.logState("nBtc", payloadDataManager.accountCount.toString())
            remoteLogger.logState("null xpub idx==$accountIndex", "hit")
        }
    }
}
