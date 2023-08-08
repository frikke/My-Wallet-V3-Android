package com.blockchain.core.payload

import com.blockchain.annotations.BurnCandidate
import com.blockchain.domain.session.SessionIdService
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.Wallet
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Balance
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.LinkedHashMap
import org.bitcoinj.core.ECKey

@BurnCandidate("Not useful")
// This class is only used my PayloadDataManager, which also has an instance on PayloadManager, and
// provides an rx wrapper for some PayloadManager calls. This is, at best, confusing and this can be merged
// into PayloadManager
internal class PayloadService(
    private val payloadManager: PayloadManager,
    private val sessionIdService: SessionIdService
) {

    // /////////////////////////////////////////////////////////////////////////
    // AUTH METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Decrypts and initializes a wallet from a payload String. Handles both V3 and V1 wallets. Will
     * return a [DecryptionException] if the password is incorrect, otherwise can return a
     * [HDWalletException] which should be regarded as fatal.
     *
     * @param payload The payload String to be decrypted
     * @param password The user's password
     * @return A [Completable] object
     */
    internal fun initializeFromPayload(
        payload: String,
        password: String
    ): Completable =
        Completable.fromCallable {
            payloadManager.initializeAndDecryptFromPayload(payload, password)
        }

    /**
     * Restores a HD wallet from a 12 word mnemonic and initializes the [PayloadDataManager].
     * Also creates a new Blockchain.info account in the process.
     *
     * @param mnemonic The 12 word mnemonic supplied as a String of words separated by whitespace
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param password The user's choice of password
     * @return An [Observable] wrapping the [Wallet] object
     */
    internal fun restoreHdWallet(
        mnemonic: String,
        walletName: String,
        email: String,
        password: String
    ): Single<Wallet> =
        payloadManager.recoverFromMnemonic(
            mnemonic,
            walletName,
            email,
            password
        )

    /**
     * Creates a new HD wallet and Blockchain.info account.
     *
     * @param password The user's choice of password
     * @param walletName The name of the wallet, usually a default name localised by region
     * @param email The user's email address, preferably not associated with another account
     * @param recaptchaToken The generated token during registration
     * @return An [Observable] wrapping the [Wallet] object
     */
    internal fun createHdWallet(
        password: String,
        walletName: String,
        email: String,
        recaptchaToken: String?
    ): Single<Wallet> =
        payloadManager.create(
            walletName,
            email,
            password,
            recaptchaToken
        )

    /**
     * Fetches the user's wallet payload, and then initializes and decrypts a payload using the
     * user's password.
     *
     * @param sharedKey The shared key as a String
     * @param guid The user's GUID
     * @param password The user's password
     * @return A [Completable] object
     */
    internal fun initializeAndDecrypt(
        sharedKey: String,
        guid: String,
        password: String
    ): Completable = sessionIdService.sessionId().flatMapCompletable {
        payloadManager.initializeAndDecrypt(
            sharedKey,
            guid,
            password,
            sessionId = it
        )
    }

    /**
     * Initializes and decrypts a user's payload given valid QR code scan data.
     *
     * @param data A QR's URI for pairing
     * @return A [Completable] object
     */
    internal fun handleQrCode(
        data: String
    ): Completable = sessionIdService.sessionId().flatMapCompletable { sId ->
        payloadManager.initializeAndDecryptFromQR(
            data,
            sId
        )
    }

    /**
     * Returns a [Completable] which saves the current payload to the server whilst also
     * forcing the sync of the user's keys. This method generates 20 addresses per [Account],
     * so it should be used only when strictly necessary (for instance, after enabling
     * notifications).
     *
     * @return A [Completable] object
     */
    internal fun syncPayloadAndPublicKeys(): Completable =
        payloadManager.syncPubKeys()

    // /////////////////////////////////////////////////////////////////////////
    // TRANSACTION METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns [Completable] which updates transactions in the PayloadManager.
     * Completable returns no value, and is used to call functions that return void but have side
     * effects.
     *
     * @return A [Completable] object
     */
    internal fun updateAllTransactions(): Completable = Completable.fromCallable {
        payloadManager.getAllTransactions(50, 0)
    }

    /**
     * Returns a [Completable] which updates all balances in the PayloadManager. Completable
     * returns no value, and is used to call functions that return void but have side effects.
     *
     * @return A [Completable] object
     */
    internal fun updateAllBalances(): Completable = Completable.fromCallable {
        payloadManager.updateAllBalances()
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes Transaction notes
     * @return A [Completable] object
     */
    internal fun updateTransactionNotes(transactionHash: String, notes: String): Completable {
        return payloadManager.updateNotesForTxHash(transactionHash, notes)
    }

    // /////////////////////////////////////////////////////////////////////////
    // ACCOUNTS AND ADDRESS METHODS
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns a [LinkedHashMap] of [Balance] objects keyed to their Bitcoin cash
     * addresses.
     *
     * @param xpubs A List of Bitcoin Cash addresses as Strings
     * @return A [LinkedHashMap]
     */
    internal fun getBalanceOfBchAccounts(xpubs: List<XPubs>): Observable<Map<String, Balance>> =
        Observable.fromCallable { payloadManager.getBalanceOfBchAccounts(xpubs) }

    /**
     * Derives new [Account] from the master seed
     *
     * @param accountLabel A label for the account
     * @param secondPassword An optional double encryption password
     * @return An [Observable] wrapping the newly created Account
     */
    internal fun createNewAccount(
        accountLabel: String,
        secondPassword: String?
    ): Observable<Account> =
        payloadManager.addAccount(accountLabel, secondPassword).toObservable()

    /**
     * Sets a private key for an associated [ImportedAddress] which is already in the [Wallet] as a
     * watch only address
     *
     * @param key An [ECKey]
     * @param secondPassword An optional double encryption password
     * @return An [Observable] representing a successful save
     */
    internal fun setKeyForImportedAddress(
        key: SigningKey,
        secondPassword: String?
    ): Observable<ImportedAddress> =
        payloadManager.setKeyForImportedAddress(key, secondPassword).toObservable()

    /**
     * Allows you to add a [ImportedAddress] to the [Wallet]
     *
     * @param importedAddress The new address
     * @return A [Completable] object representing a successful save
     */
    internal fun addImportedAddress(importedAddress: ImportedAddress): Completable =
        payloadManager.addImportedAddress(importedAddress)
}
