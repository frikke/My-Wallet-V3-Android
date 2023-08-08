package info.blockchain.wallet.payload

import com.blockchain.AppVersion
import com.blockchain.api.blockchainApiModule
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.data.DataResource
import com.blockchain.testutils.KoinTestRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.Device
import info.blockchain.wallet.ImportedAddressHelper.getImportedAddress
import info.blockchain.wallet.WalletApiMockedResponseTest
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.keys.SigningKeyImpl
import info.blockchain.wallet.multiaddress.MultiAddressFactoryBtc
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto
import info.blockchain.wallet.payload.store.PayloadDataStore
import io.reactivex.rxjava3.core.Completable
import java.math.BigInteger
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.DeterministicKey
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import retrofit2.HttpException
import retrofit2.Response

class PayloadManagerTest : WalletApiMockedResponseTest(), KoinTest {
    private val bitcoinApi = Mockito.mock(
        NonCustodialBitcoinService::class.java
    )
    private val walletApi: WalletApi = mock()
    private val payloadDataStore: PayloadDataStore = mock()

    private lateinit var payloadManager: PayloadManager

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            blockchainApiModule
        )
    }
    val json: Json by inject()

    @Before fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(
            walletApi.updateWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        payloadManager = PayloadManager(
            walletApi,
            payloadDataStore,
            bitcoinApi,
            mock(),
            MultiAddressFactoryBtc(bitcoinApi),
            BalanceManagerBtc(bitcoinApi),
            BalanceManagerBch(bitcoinApi),
            object : Device {
                override val osType: String
                    get() = "android"
            },
            mock(),
            object : AppVersion {
                override val appVersion: String
                    get() = "8.18"
            }
        )
    }

    @Test
    fun create_v4() {
        mockEmptyBalance(bitcoinApi)
        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            "CAPTCHA"
        ).test()
        val walletBody = payloadManager.payload
        Assert.assertEquals(36, walletBody.guid.length.toLong()) // GUIDs are 36 in length
        Assert.assertEquals("My HDWallet", walletBody.walletBody!!.accounts[0].label)
        Assert.assertEquals(1, walletBody.walletBody!!.accounts.size.toLong())
        assert(walletBody.options.pbkdf2Iterations != null)
        Assert.assertEquals(5000, (walletBody.options.pbkdf2Iterations as Int).toLong())
        Assert.assertEquals(600000, walletBody.options.logoutTime)
        assert(walletBody.options.feePerKb != null)
        Assert.assertEquals(10000, walletBody.options.feePerKb as Long)
    }

    @Test
    fun create_ServerConnectionException() {
        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.error(HttpException(Response.error<String>(500, ResponseBody.create(null, "")))))
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            "CAPTCHA"
        ).test().assertError(ServerConnectionException::class.java)
    }

    @Test
    fun recoverFromMnemonic_v4() {
        val mnemonic = "all all all all all all all all all all all all"

        // Responses for checking how many accounts to recover
        val balance1 = loadResourceContent("balance/wallet_all_balance_1.txt")
        val balanceResponse1 = makeBalanceResponse(balance1)
        val balance2 = loadResourceContent("balance/wallet_all_balance_2.txt")
        val balanceResponse2 = makeBalanceResponse(balance2)
        val balance3 = loadResourceContent("balance/wallet_all_balance_3.txt")
        val balanceResponse3 = makeBalanceResponse(balance3)
        val balance4 = loadResourceContent("balance/wallet_all_balance_4.txt")
        val balanceResponse4 = makeBalanceResponse(balance4)
        val balance5 = loadResourceContent("balance/wallet_all_balance_5.txt")
        val balanceResponse5 = makeBalanceResponse(balance5)
        Mockito.`when`(
            bitcoinApi.getBalance(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3)
            .thenReturn(balanceResponse4)
            .thenReturn(balanceResponse5)

        payloadManager.recoverFromMnemonic(
            mnemonic,
            "My HDWallet",
            "name@email.com",
            "SomePassword"
        ).test()

        val walletBody = payloadManager
            .payload

        Assert.assertEquals(36, walletBody.guid.length.toLong()) // GUIDs are 36 in length
        Assert.assertEquals("My HDWallet", walletBody.walletBody!!.accounts[0].label)
        Assert.assertEquals("0660cc198330660cc198330660cc1983", walletBody.walletBody!!.seedHex)
        Assert.assertEquals(14, walletBody.walletBody!!.accounts.size)
        Assert.assertEquals(5000, walletBody.options.pbkdf2Iterations)
        Assert.assertEquals(600000, walletBody.options.logoutTime)
        Assert.assertEquals(10000, walletBody.options.feePerKb as Long)
    }

    @Test
    fun recoverFromMnemonic_ServerConnectionException_v3() {
        val mnemonic = "all all all all all all all all all all all all"

        // checking if xpubs has txs succeeds but then saving fails
        // Responses for checking how many accounts to recover
        val balance1 = loadResourceContent("balance/wallet_all_balance_1.txt")
        val balanceResponse1 = makeBalanceResponse(balance1)
        val balance2 = loadResourceContent("balance/wallet_all_balance_2.txt")
        val balanceResponse2 = makeBalanceResponse(balance2)
        val balance3 = loadResourceContent("balance/wallet_all_balance_3.txt")
        val balanceResponse3 = makeBalanceResponse(balance3)
        val balance4 = loadResourceContent("balance/wallet_all_balance_4.txt")
        val balanceResponse4 = makeBalanceResponse(balance4)
        val balance5 = loadResourceContent("balance/wallet_all_balance_5.txt")
        val balanceResponse5 = makeBalanceResponse(balance5)
        Mockito.`when`(
            bitcoinApi.getBalance(
                any(),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3)
            .thenReturn(balanceResponse4)
            .thenReturn(balanceResponse5)

        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            Completable.error(HttpException(Response.error<String>(500, ResponseBody.Companion.create(null, ""))))
        )

        val testRecoverPayload = payloadManager.recoverFromMnemonic(
            mnemonic,
            "My HDWallet",
            "name@email.com",
            "SomePassword"
        ).test().await()

        testRecoverPayload.assertError(
            ServerConnectionException::class.java
        )
    }

    @Test
    fun initializeAndDecrypt_unsupported_version_v4() {
        val walletBase = loadResourceContent("wallet_v5_unsupported.txt")
        whenever(
            payloadDataStore.stream(
                any()
            )
        ).thenReturn(
            flowOf(
                DataResource.Data(json.decodeFromString(WalletBaseDto.serializer(), walletBase))
            )
        )
        payloadManager.initializeAndDecrypt(
            "any_shared_key",
            "any_guid",
            "SomeTestPassword",
            "sid"
        ).test().assertError(UnsupportedVersionException::class.java)
    }

    @Test
    fun initializeAndDecrypt_v4() {
        val walletBase = loadResourceContent("wallet_v4_encrypted.txt")
        mockEmptyBalance(bitcoinApi)

        whenever(
            payloadDataStore.stream(
                any()
            )
        ).thenReturn(
            flowOf(
                DataResource.Data(json.decodeFromString(WalletBaseDto.serializer(), walletBase))
            )
        )

        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "blockchain",
            "sid"
        ).test().assertComplete()
    }

    @Test
    fun addLegacyAddress_v3() {
        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        whenever(
            walletApi.updateWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        mockEmptyBalance(bitcoinApi)
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            "CAPTCHA"
        ).test()
        Assert.assertEquals(0, payloadManager.payload.importedAddressList.size)
        payloadManager.addImportedAddress(getImportedAddress()).test()
        Assert.assertEquals(1, payloadManager.payload.importedAddressList.size)
        payloadManager.addImportedAddress(getImportedAddress()).test()
        Assert.assertEquals(2, payloadManager.payload.importedAddressList.size)
    }

    @Test
    fun setKeyForLegacyAddress_v3() {
        mockEmptyBalance(bitcoinApi)

        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())
        whenever(
            walletApi.updateWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            "CAPTCHA"
        ).test()
        Assert.assertEquals(0, payloadManager.payload.importedAddressList.size.toLong())
        payloadManager.addImportedAddress(getImportedAddress()).test()
        Assert.assertEquals(1, payloadManager.payload.importedAddressList.size)

        val importedAddressBody = payloadManager.payload.importedAddressList[0]

        val signingKey = SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(importedAddressBody.privateKey))
        )
        payloadManager.setKeyForImportedAddress(signingKey, null).test()
        Assert.assertEquals(importedAddressBody.privateKey, "tb1TutW9CCZUqsXQ9nhvatCW51sauRJapY5YpW3zddF")
    }

    @Test
    fun initializeAndDecrypt_invalidGuid() {
        val walletBase = loadResourceContent("invalid_guid.txt")
        whenever(payloadDataStore.stream(any())).thenReturn(
            flowOf(
                DataResource.Error(
                    HttpException(
                        Response.error<String>(500, walletBase.toResponseBody("application/json".toMediaTypeOrNull()))
                    )
                )
            )
        )
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "SomeTestPassword",
            "sid"
        ).test().assertError(InvalidCredentialsException::class.java)
    }

    @Test
    fun setKeyForLegacyAddress_NoSuchAddressException() {
        mockEmptyBalance(bitcoinApi)

        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())
        whenever(
            walletApi.updateWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Completable.complete())

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            "CAPTCHA"
        ).test()
        Assert.assertEquals(0, payloadManager.payload.importedAddressList.size.toLong())
        payloadManager.addImportedAddress(getImportedAddress()).test()
        Assert.assertEquals(1, payloadManager.payload.importedAddressList.size.toLong())
        val (address, privateKey) = payloadManager.payload.importedAddressList[0]

        // Try non matching ECKey
        val key: SigningKey = SigningKeyImpl(ECKey())
        val newlyAdded = payloadManager
            .setKeyForImportedAddress(key, null).test()
        // Ensure new address is created if no match found
        newlyAdded.assertValue {
            it.privateKey != null && it.privateKey != privateKey && it.address != address
        }
    }

    @Test
    fun addAccount_v4() {
        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            Completable.complete()
        )

        whenever(
            walletApi.updateWallet(
                any(),
                any(),
                any(),
                anyString(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Completable.complete()
        )

        mockEmptyBalance(bitcoinApi)

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            "CAPTCHA"
        ).test()

        Assert.assertEquals(1, payloadManager.payload!!.walletBody!!.accounts.size.toLong())
        mockEmptyBalance(bitcoinApi)
        payloadManager.addAccount("Some Label", null).test()
        Assert.assertEquals(2, payloadManager.payload!!.walletBody!!.accounts.size.toLong())
        payloadManager.addAccount("Some Label", null).test().await()
        Assert.assertEquals(3, payloadManager.payload!!.walletBody!!.accounts.size.toLong())
    }

    @Test
    fun save_v4() {
        mockEmptyBalance(bitcoinApi)
        whenever(
            walletApi.insertWallet(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyString(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(
            Completable.complete()
        )
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            "CAPTCHA"
        ).test().assertComplete()
    }

    @Test fun upgradeV2PayloadToV3() {
        // Tested in integration tests
    }

    // Reserve an address to ensure it gets skipped
    @Test
    fun nextAddress_v3() {
        // set up indexes first

        // Next Receive

        // Increment receive and check

        // Next Change

        // Increment Change and check

        val walletBase = loadResourceContent("wallet_v3_5.txt")

        mockEmptyBalance(bitcoinApi)
        val multi1 = loadResourceContent("multiaddress/wallet_v3_5_m1.txt")
        val multiResponse1 = makeMultiAddressResponse(multi1)
        val multi2 = loadResourceContent("multiaddress/wallet_v3_5_m2.txt")
        val multiResponse2 = makeMultiAddressResponse(multi2)
        val multi3 = loadResourceContent("multiaddress/wallet_v3_5_m3.txt")
        val multiResponse3 = makeMultiAddressResponse(multi3)
        val multi4 = loadResourceContent("multiaddress/wallet_v3_5_m4.txt")
        val multiResponse4 = makeMultiAddressResponse(multi4)
        Mockito.`when`(
            bitcoinApi.getMultiAddress(
                any(),
                any(),
                any(),
                eq(null),
                any(),
                any(),
                any()
            )
        ).thenReturn(multiResponse1)
            .thenReturn(multiResponse2)
            .thenReturn(multiResponse3)
            .thenReturn(multiResponse4)

        whenever(
            payloadDataStore.stream(
                any()
            )
        ).thenReturn(
            flowOf(
                DataResource.Data(
                    json.decodeFromString(WalletBaseDto.serializer(), walletBase)
                )
            )
        )

        payloadManager.initializeAndDecrypt(
            "06f6fa9c-d0fe-403d-815a-111ee26888e2",
            "4750d125-5344-4b79-9cf9-6e3c97bc9523",
            "MyTestWallet",
            "sid"
        ).test().assertComplete()
        val wallet = payloadManager.payload

        // Reserve an address to ensure it gets skipped
        val account = wallet.walletBody!!.accounts[0].addAddressLabel(1, "Reserved")

        // set up indexes first
        payloadManager.getAccountTransactions(
            account.xpubs,
            50,
            0
        )

        // Next Receive
        var nextReceiveAddress = payloadManager.getNextReceiveAddress(account)
        Assert.assertEquals("1H9FdkaryqzB9xacDbJrcjXsJ9By4UVbQw", nextReceiveAddress)

        // Increment receive and check
        payloadManager.incrementNextReceiveAddress(account)
        nextReceiveAddress = payloadManager.getNextReceiveAddress(account)
        Assert.assertEquals("18DU2RjyadUmRK7sHTBHtbJx5VcwthHyF7", nextReceiveAddress)

        // Next Change
        var nextChangeAddress = payloadManager.getNextChangeAddress(account)
        Assert.assertEquals("1GEXfMa4SMh3iUZxP8HHQy7Wo3aqce72Nm", nextChangeAddress)

        // Increment Change and check
        payloadManager.incrementNextChangeAddress(account)
        nextChangeAddress = payloadManager.getNextChangeAddress(account)
        Assert.assertEquals("1NzpLHV6LLVFCYdYA5woYL9pHJ48KQJc9K", nextChangeAddress)
    }

    @Test fun balance() {
        val walletBase = loadResourceContent("wallet_v3_6.txt")

        whenever(
            payloadDataStore.stream(
                any()
            )
        ).thenReturn(
            flowOf(
                DataResource.Data(json.decodeFromString(WalletBaseDto.serializer(), walletBase))
            )
        )

        // Bitcoin
        val btcBalance = loadResourceContent("balance/wallet_v3_6_balance.txt")
        val btcResponse = makeBalanceResponse(btcBalance)
        Mockito.`when`(
            bitcoinApi.getBalance(
                eq("btc"),
                any(),
                any(),
                any()
            )
        ).thenReturn(btcResponse)

        // Bitcoin Cash
        val bchBalance = loadResourceContent("balance/wallet_v3_6_balance.txt")
        val bchResponse = makeBalanceResponse(bchBalance)
        Mockito.`when`(
            bitcoinApi.getBalance(
                eq("bch"),
                any(),
                any(),
                any()
            )
        ).thenReturn(bchResponse)
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "MyTestWallet",
            "sid"
        ).test().assertComplete()
        payloadManager.updateAllBalances()

        // 'All' wallet balance and transactions
        Assert.assertEquals(743071, payloadManager.getWalletBalance().toLong())
        val balance = payloadManager.importedAddressesBalance
        // Imported addresses consolidated
        Assert.assertEquals(137505, balance.toLong())

        // Account and address balances
        val first = XPubs(
            XPub(
                "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV",
                XPub.Format.LEGACY
            )
        )
        Assert.assertEquals(
            BigInteger.valueOf(566349),
            payloadManager.getAddressBalance(first).toBigInteger()
        )
        val second = XPubs(
            XPub(
                "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL",
                XPub.Format.LEGACY
            )
        )
        Assert.assertEquals(
            BigInteger.valueOf(39217),
            payloadManager.getAddressBalance(second).toBigInteger()
        )
        val third = XPubs(
            XPub(
                "189iKJLruPtUorasDuxmc6fMRVxz6zxpPS",
                XPub.Format.LEGACY
            )
        )
        Assert.assertEquals(
            BigInteger.valueOf(137505),
            payloadManager.getAddressBalance(third).toBigInteger()
        )
    }

    // guid 5350e5d5-bd65-456f-b150-e6cc089f0b26
    @Test
    fun accountTransactions() {
        // Bitcoin

        // Bitcoin Cash

        // Bitcoin
        // Bitcoin cash
        // Account 1
        // My Bitcoin Account
        // Savings account
        // My Bitcoin Wallet
        // My Bitcoin Wallet
        // My Bitcoin Wallet
        // My Bitcoin Wallet
        // My Bitcoin Wallet
        // My Bitcoin Wallet
        // Savings account
        // My Bitcoin Wallet

        // Account 2
        // My Bitcoin Wallet
        // Savings account
        // My Bitcoin Wallet
        // Savings account

        val walletBase = loadResourceContent("wallet_v3_6.txt")
        whenever(
            payloadDataStore.stream(
                any()
            )
        ).thenReturn(
            flowOf(
                DataResource.Data(
                    json.decodeFromString(WalletBaseDto.serializer(), walletBase)
                )
            )
        )

        // Bitcoin
        val btcBalance = loadResourceContent("balance/wallet_v3_6_balance.txt")
        val btcBalanceResponse = makeBalanceResponse(btcBalance)
        Mockito.`when`(
            bitcoinApi.getBalance(
                eq("btc"),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(btcBalanceResponse)

        // Bitcoin Cash
        val bchBalance = loadResourceContent("balance/wallet_v3_6_balance.txt")
        val bchBalanceResponse = makeBalanceResponse(bchBalance)
        Mockito.`when`(
            bitcoinApi.getBalance(
                eq("bch"),
                any(),
                any(),
                any()
            )
        )
            .thenReturn(bchBalanceResponse)

        // Bitcoin
        mockMultiAddress(bitcoinApi, "btc", "multiaddress/wallet_v3_6_m1.txt")
        // Bitcoin cash
        mockMultiAddress(bitcoinApi, "bch", "multiaddress/wallet_v3_6_m1.txt")
        payloadManager.initializeAndDecrypt(
            "0f28735d-0b89-405d-a40f-ee3e85c3c78c",
            "5350e5d5-bd65-456f-b150-e6cc089f0b26",
            "MyTestWallet",
            "sid"
        ).test().assertComplete()

        // Account 1
        val first = XPubs(
            XPub(
                "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV",
                XPub.Format.LEGACY
            )
        )
        mockMultiAddress(bitcoinApi, "multiaddress/wallet_v3_6_m2.txt")
        var transactionSummaries = payloadManager
            .getAccountTransactions(first, 50, 0)
        Assert.assertEquals(8, transactionSummaries.size.toLong())
        var summary = transactionSummaries[0]
        Assert.assertEquals(68563, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU")) // My Bitcoin Account
        Assert.assertEquals(2, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw")) // Savings account
        Assert.assertTrue(summary.outputsMap.containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"))
        summary = transactionSummaries[1]
        Assert.assertEquals(138068, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.SENT, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ")) // My Bitcoin Wallet
        Assert.assertEquals(2, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1LQwNvEMnYjNCNxeUJzDfD8mcSqhm2ouPp"))
        Assert.assertTrue(summary.outputsMap.containsKey("1AdTcerDBY735kDhQWit5Scroae6piQ2yw"))
        summary = transactionSummaries[2]
        Assert.assertEquals(800100, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.RECEIVED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("19CMnkUgBnTBNiTWXwoZr6Gb3aeXKHvuGG"))
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ")) // My Bitcoin Wallet
        summary = transactionSummaries[3]
        Assert.assertEquals(35194, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.SENT, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("15HjFY96ZANBkN5kvPRgrXH93jnntqs32n")) // My Bitcoin Wallet
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1PQ9ZYhv9PwbWQQN74XRqUCjC32JrkyzB9"))
        summary = transactionSummaries[4]
        Assert.assertEquals(98326, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ")) // My Bitcoin Wallet
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"))
        summary = transactionSummaries[5]
        Assert.assertEquals(160640, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.RECEIVED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("1BZe6YLaf2HiwJdnBbLyKWAqNia7foVe1w"))
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ")) // My Bitcoin Wallet
        summary = transactionSummaries[6]
        Assert.assertEquals(9833, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur")) // My Bitcoin Wallet
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r")) // Savings account
        summary = transactionSummaries[7]
        Assert.assertEquals(40160, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.RECEIVED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("1Baa1cjB1CyBVSjw8SkFZ2YBuiwKnKLXhe"))
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur")) // My Bitcoin Wallet

        // Account 2
        val second = XPubs(
            XPub(
                "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL",
                XPub.Format.LEGACY
            )
        )
        mockMultiAddress(bitcoinApi, "multiaddress/wallet_v3_6_m3.txt")
        transactionSummaries = payloadManager.getAccountTransactions(second, 50, 0)
        Assert.assertEquals(2, transactionSummaries.size.toLong())
        summary = transactionSummaries[0]
        Assert.assertEquals(68563, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU")) // My Bitcoin Wallet
        Assert.assertEquals(2, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw")) // Savings account
        Assert.assertTrue(summary.outputsMap.containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"))
        summary = transactionSummaries[1]
        Assert.assertEquals(9833, summary.total.toLong())
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.transactionType)
        Assert.assertEquals(1, summary.inputsMap.size.toLong())
        Assert.assertTrue(summary.inputsMap.containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur")) // My Bitcoin Wallet
        Assert.assertEquals(1, summary.outputsMap.size.toLong())
        Assert.assertTrue(summary.outputsMap.containsKey("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r")) // Savings account
    }
}
