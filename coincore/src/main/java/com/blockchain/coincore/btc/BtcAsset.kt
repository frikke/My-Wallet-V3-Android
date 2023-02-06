package com.blockchain.coincore.btc

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.IdentityAddressResolver
import com.blockchain.coincore.MultipleWalletsAsset
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.coincore.impl.CryptoAssetBase
import com.blockchain.coincore.impl.NotificationAddresses
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

internal class BtcAsset(
    private val payloadManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatusPrefs,
    private val notificationUpdater: BackendNotificationUpdater,
    private val addressResolver: IdentityAddressResolver,
) : CryptoAssetBase(),
    MultipleWalletsAsset {

    override val currency: AssetInfo
        get() = CryptoCurrency.BTC

    override fun loadNonCustodialAccounts(labels: DefaultLabels): Single<SingleAccountList> =
        Single.fromCallable {
            with(payloadManager) {
                val result = mutableListOf<CryptoAccount>()
                accounts.forEachIndexed { i, account ->
                    val btcAccount = btcAccountFromPayloadAccount(i, account)
                    if (btcAccount.isDefault) {
                        updateBackendNotificationAddresses(btcAccount)
                    }
                    result.add(btcAccount)
                }

                importedAddresses.forEach { account ->
                    result.add(btcAccountFromImportedAccount(account))
                }
                result
            }
        }

    private fun updateBackendNotificationAddresses(account: BtcCryptoWalletAccount) {
        require(account.isDefault)
        require(!account.isArchived)

        val addressList = mutableListOf<String>()

        for (i in 0 until OFFLINE_CACHE_ITEM_COUNT) {
            account.getReceiveAddressAtPosition(i)?.let {
                addressList += it
            }
        }

        val notify = NotificationAddresses(
            assetTicker = currency.networkTicker,
            addressList = addressList
        )
        return notificationUpdater.updateNotificationBackend(notify)
    }

    override fun parseAddress(address: String, label: String?, isDomainAddress: Boolean): Maybe<ReceiveAddress> =
        Maybe.fromCallable {
            // Remove any potential trailing white spaces
            val normalisedAddress = address.removePrefix(FormatsUtil.BTC_PREFIX).trim()
            val parts = normalisedAddress.split("?")
            val addressPart = parts.getOrNull(0)
            val amountPart = parts.find {
                it.startsWith(BTC_ADDRESS_AMOUNT_PART, true)
            }?.let {
                val amountString = it.removePrefix(BTC_ADDRESS_AMOUNT_PART)
                if (amountString.isNotEmpty()) {
                    CryptoValue.fromMajor(currency, amountString.toBigDecimal())
                } else {
                    null
                }
            }
            if (addressPart != null && isValidAddress(addressPart)) {
                BtcAddress(
                    address = addressPart,
                    label = label ?: address,
                    isDomain = isDomainAddress,
                    amount = amountPart
                )
            } else {
                null
            }
        }

    override fun isValidAddress(address: String): Boolean =
        sendDataManager.isValidBtcAddress(address)

    fun createAccount(label: String, secondPassword: String?): Single<BtcCryptoWalletAccount> =
        payloadManager.createNewAccount(label, secondPassword)
            .singleOrError()
            .map { btcAccountFromPayloadAccount(payloadManager.accountCount - 1, it) }
            .doOnSuccess { forceAccountsRefresh() }

    override fun createWalletFromLabel(label: String, secondPassword: String?): Single<BtcCryptoWalletAccount> =
        payloadManager.createNewAccount(label, secondPassword)
            .singleOrError()
            .map { btcAccountFromPayloadAccount(payloadManager.accountCount - 1, it) }
            .doOnSuccess { forceAccountsRefresh() }

    override fun createWalletFromAddress(address: String): Completable =
        throw UnsupportedOperationException("Action not supported")

    override fun importWalletFromKey(
        keyData: String,
        keyFormat: String,
        keyPassword: String?,
        walletSecondPassword: String?
    ): Single<BtcCryptoWalletAccount> {
        require(keyData.isNotEmpty())
        require(keyPassword != null || keyFormat != PrivateKeyFactory.BIP38)

        return when (keyFormat) {
            PrivateKeyFactory.BIP38 -> extractBip38Key(keyData, keyPassword!!)
            else -> extractKey(keyData, keyFormat)
        }.map { key ->
            if (!key.hasPrivKey)
                throw Exception()
            key
        }.flatMap { key ->
            payloadManager.addImportedAddressFromKey(key, walletSecondPassword)
        }.map { importedAddress ->
            btcAccountFromImportedAccount(importedAddress)
        }.doOnSuccess {
            forceAccountsRefresh()
        }
    }

    private fun extractBip38Key(keyData: String, keyPassword: String): Single<SigningKey> =
        payloadManager.getBip38KeyFromImportedData(keyData, keyPassword)

    private fun extractKey(keyData: String, keyFormat: String): Single<SigningKey> =
        payloadManager.getKeyFromImportedData(keyFormat, keyData)

    private fun btcAccountFromPayloadAccount(index: Int, payloadAccount: Account): BtcCryptoWalletAccount =
        BtcCryptoWalletAccount.createHdAccount(
            jsonAccount = payloadAccount,
            hdAccountIndex = index,
            payloadDataManager = payloadManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            walletPreferences = walletPreferences,
            refreshTrigger = this,
            addressResolver = addressResolver
        )

    private fun btcAccountFromImportedAccount(payloadAccount: ImportedAddress): BtcCryptoWalletAccount =
        BtcCryptoWalletAccount.createImportedAccount(
            importedAccount = payloadAccount,
            payloadDataManager = payloadManager,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            walletPreferences = walletPreferences,
            refreshTrigger = this,
            addressResolver = addressResolver
        )

    companion object {
        private const val OFFLINE_CACHE_ITEM_COUNT = 5
        private const val BTC_ADDRESS_AMOUNT_PART = "amount="
    }
}

internal class BtcAddress(
    override val address: String,
    override val label: String = address,
    override val isDomain: Boolean = false,
    override val onTxCompleted: (TxResult) -> Completable = { Completable.complete() },
    override val amount: CryptoValue? = null
) : CryptoAddress {
    override val asset: AssetInfo = CryptoCurrency.BTC

    override fun toUrl(amount: Money): String {
        return FormatsUtil.toBtcUri(address, amount.toBigInteger())
    }
}
