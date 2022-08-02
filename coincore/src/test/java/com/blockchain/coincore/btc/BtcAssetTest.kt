package com.blockchain.coincore.btc

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.impl.BackendNotificationUpdater
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import exchange.ExchangeLinking
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.ImportedAddress
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.bitcoinj.crypto.BIP38PrivateKey.BadPassphraseException
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager

class BtcAssetTest : KoinTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val payloadManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val walletPreferences: WalletStatusPrefs = mock()
    private val notificationUpdater: BackendNotificationUpdater = mock()

    private val subject = BtcAsset(
        payloadManager = payloadManager,
        sendDataManager = sendDataManager,
        feeDataManager = feeDataManager,
        notificationUpdater = notificationUpdater,
        walletPreferences = walletPreferences,
        addressResolver = mock()
    )

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(mockAssetDependenciesModule)
    }

    @Test
    fun createAccountSuccessNoSecondPassword() {
        val mockInternalAccount: Account = mock {
            on { xpubs }.thenReturn(XPubs(listOf(XPub(NEW_XPUB, XPub.Format.LEGACY))))
        }

        whenever(payloadManager.createNewAccount(TEST_LABEL, null)).thenReturn(
            Observable.just(mockInternalAccount)
        )
        whenever(payloadManager.accountCount).thenReturn(NUM_ACCOUNTS)

        subject.createAccount(TEST_LABEL, null)
            .test()
            .assertValue {
                it.isHDAccount &&
                    !it.isArchived &&
                    it.xpubAddress == NEW_XPUB
            }.assertComplete()
    }

    @Test
    fun createAccountFailed() {
        whenever(payloadManager.createNewAccount(TEST_LABEL, null)).thenReturn(
            Observable.error(Exception("Something went wrong"))
        )

        subject.createAccount(TEST_LABEL, null)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun importNonBip38Success() {
        val ecKey: SigningKey = mock {
            on { hasPrivKey }.thenReturn(true)
        }

        val internalAccount: ImportedAddress = mock {
            on { address }.thenReturn(IMPORTED_ADDRESS)
        }

        whenever(payloadManager.getKeyFromImportedData(NON_BIP38_FORMAT, KEY_DATA))
            .thenReturn(Single.just(ecKey))
        whenever(payloadManager.addImportedAddressFromKey(ecKey, null))
            .thenReturn(Single.just(internalAccount))

        subject.importWalletFromKey(KEY_DATA, NON_BIP38_FORMAT, null, null)
            .test()
            .assertValue {
                !it.isHDAccount &&
                    !it.isArchived &&
                    it.xpubAddress == IMPORTED_ADDRESS
            }.assertComplete()
    }

    @Test
    fun importNonBip38NoPrivateKey() {
        val ecKey: SigningKey = mock {
            on { hasPrivKey }.thenReturn(true)
        }

        whenever(payloadManager.getKeyFromImportedData(NON_BIP38_FORMAT, KEY_DATA))
            .thenReturn(Single.just(ecKey))

        subject.importWalletFromKey(KEY_DATA, NON_BIP38_FORMAT, null, null)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun importNonBip38InvalidFormat() {
        whenever(payloadManager.getKeyFromImportedData(NON_BIP38_FORMAT, KEY_DATA))
            .thenReturn(Single.error(Exception()))

        subject.importWalletFromKey(KEY_DATA, NON_BIP38_FORMAT, null, null)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun importBip38Success() {
        val ecKey: SigningKey = mock {
            on { hasPrivKey }.thenReturn(true)
        }

        val internalAccount: ImportedAddress = mock {
            on { address }.thenReturn(IMPORTED_ADDRESS)
        }

        whenever(payloadManager.getBip38KeyFromImportedData(KEY_DATA, KEY_PASSWORD))
            .thenReturn(Single.just(ecKey))
        whenever(payloadManager.addImportedAddressFromKey(ecKey, null))
            .thenReturn(Single.just(internalAccount))

        subject.importWalletFromKey(KEY_DATA, BIP38_FORMAT, KEY_PASSWORD, null)
            .test()
            .assertValue {
                !it.isHDAccount &&
                    !it.isArchived &&
                    it.xpubAddress == IMPORTED_ADDRESS
            }.assertComplete()
    }

    @Test
    fun importBip38BadPassword() {
        whenever(payloadManager.getBip38KeyFromImportedData(KEY_DATA, KEY_PASSWORD))
            .thenReturn(Single.error(BadPassphraseException()))

        subject.importWalletFromKey(KEY_DATA, BIP38_FORMAT, KEY_PASSWORD, null)
            .test()
            .assertError(BadPassphraseException::class.java)
    }

    @Test
    fun parseGoodAddressWithNoPrefix() {
        val goodAddress = "17GBRdfBHtEaBs7MesvMgob6YUEn5fFN4C"

        whenever(sendDataManager.isValidBtcAddress(goodAddress)).thenReturn(true)

        subject.parseAddress(goodAddress, TEST_LABEL)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { v ->
                v is BtcAddress &&
                    v.address == goodAddress &&
                    v.amount == null &&
                    v.label == TEST_LABEL
            }
    }

    @Test
    fun parseGoodAddressWithPrefix() {
        val prefix = "bitcoin:"
        val goodAddress = "17GBRdfBHtEaBs7MesvMgob6YUEn5fFN4C"
        val testInput = "$prefix$goodAddress"

        whenever(sendDataManager.isValidBtcAddress(goodAddress)).thenReturn(true)

        subject.parseAddress(testInput, TEST_LABEL)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { v ->
                v is BtcAddress &&
                    v.address == goodAddress &&
                    v.amount == null &&
                    v.label == TEST_LABEL
            }
    }

    @Test
    fun parseGoodAddressWithPrefixAndAmount() {
        val prefix = "bitcoin:"
        val goodAddress = "17GBRdfBHtEaBs7MesvMgob6YUEn5fFN4C"
        val amount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.004409.toBigDecimal())
        val testInput = "$prefix$goodAddress?amount=${amount.toStringWithoutSymbol()}"

        whenever(sendDataManager.isValidBtcAddress(goodAddress)).thenReturn(true)

        subject.parseAddress(testInput, TEST_LABEL)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { v ->
                v is BtcAddress &&
                    v.address == goodAddress &&
                    v.amount == amount &&
                    v.label == TEST_LABEL
            }
    }

    @Test
    fun parseGoodAddressWithPrefixAndUnknownSuffix() {

        val prefix = "bitcoin:"
        val goodAddress = "17GBRdfBHtEaBs7MesvMgob6YUEn5fFN4C"
        val amount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.004409.toBigDecimal())
        val testInput = "$prefix$goodAddress?unknown=${amount.toStringWithoutSymbol()}"

        whenever(sendDataManager.isValidBtcAddress(goodAddress)).thenReturn(true)

        subject.parseAddress(testInput, TEST_LABEL)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { v ->
                v is BtcAddress &&
                    v.address == goodAddress &&
                    v.amount == null &&
                    v.label == TEST_LABEL
            }
    }

    @Test
    fun parseGoodAddressWithPrefixAndAmountAndUnknownSuffix() {

        val prefix = "bitcoin:"
        val goodAddress = "17GBRdfBHtEaBs7MesvMgob6YUEn5fFN4C"
        val amount = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.004409.toBigDecimal())
        val testInput = "$prefix$goodAddress?amount=${amount.toStringWithoutSymbol()}?unknown=whatever"

        whenever(sendDataManager.isValidBtcAddress(goodAddress)).thenReturn(true)

        subject.parseAddress(testInput, TEST_LABEL)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { v ->
                v is BtcAddress &&
                    v.address == goodAddress &&
                    v.amount == amount &&
                    v.label == TEST_LABEL
            }
    }

    @Test
    fun parseBadAddress() {
        val badAddress = "ThisIsNotABTCAddress"
        whenever(sendDataManager.isValidBtcAddress(badAddress)).thenReturn(false)

        subject.parseAddress(badAddress, TEST_LABEL)
            .test()
            .assertResult() // Should be empty
    }

    companion object {
        private const val TEST_LABEL = "TestLabel"
        private const val NEW_XPUB = "jaohaeoufoaehfoiuaehfiuhaefiuaeifuhaeifuh"
        private const val NUM_ACCOUNTS = 5

        private const val KEY_DATA = "aefouaoefkajdfsnkajsbkjasbdfkjbaskjbasfkj"
        private const val NON_BIP38_FORMAT = PrivateKeyFactory.BASE64
        private const val BIP38_FORMAT = PrivateKeyFactory.BIP38
        private const val KEY_PASSWORD = "SuperSecurePassword"
        private const val IMPORTED_ADDRESS = "aeoiawfohiawiawiohawdfoihawdhioadwfohiafwoih"
    }
}

private val mockAssetDependenciesModule = module {
    factory {
        mock<ExchangeRatesDataManager>()
    }
    factory {
        mock<CustodialWalletManager>()
    }

    factory {
        mock<TradingService>()
    }

    factory {
        mock<InterestService>()
    }

    factory {
        mock<ExchangeLinking>()
    }
    factory {
        mock<DefaultLabels>()
    }

    factory {
        mock<RemoteLogger>()
    }

    factory {
        mock<UserIdentity>()
    }
}
