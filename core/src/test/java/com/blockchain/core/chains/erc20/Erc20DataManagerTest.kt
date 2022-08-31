package com.blockchain.core.chains.erc20

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.erc20.call.Erc20HistoryCallCache
import com.blockchain.core.chains.erc20.data.store.Erc20DataSource
import com.blockchain.core.chains.erc20.data.store.Erc20L2DataSource
import com.blockchain.core.chains.erc20.domain.Erc20L2StoreService
import com.blockchain.core.chains.erc20.domain.Erc20StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.core.chains.erc20.domain.model.Erc20HistoryEvent
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.math.BigInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class Erc20DataManagerTest {

    private val testScheduler = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.reset()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
    }

    @After
    fun tearDown() = RxJavaPlugins.reset()

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val ethDataManager: EthDataManager = mockk {
        every { accountAddress } returns ACCOUNT_HASH
        every { supportedNetworks } returns Single.just(
            setOf(
                EvmNetwork(
                    CryptoCurrency.ETHER.networkTicker,
                    CryptoCurrency.ETHER.name,
                    EthDataManager.ETH_CHAIN_ID,
                    ""
                )
            )
        )
        coEvery { getBalance() } returns Outcome.Success(BigInteger.ZERO)
    }
    private val historyCallCache: Erc20HistoryCallCache = mock()
    private val assetCatalogue: AssetCatalogue = mockk()
    private val erc20StoreService: Erc20StoreService = mock()
    private val erc20DataSource: Erc20DataSource = mock()
    private val erc20L2StoreService: Erc20L2StoreService = mock()
    private val erc20L2DataSource: Erc20L2DataSource = mock()
    private val ethLayerTwoFeatureFlag: IntegratedFeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(false))
    }
    private val evmWithoutL1BalanceFeatureFlag: IntegratedFeatureFlag = mock {
        on { enabled }.thenReturn(Single.just(false))
    }

    private val subject = Erc20DataManagerImpl(
        ethDataManager = ethDataManager,
        historyCallCache = historyCallCache,
        assetCatalogue = assetCatalogue,
        erc20StoreService = erc20StoreService,
        erc20DataSource = erc20DataSource,
        erc20L2StoreService = erc20L2StoreService,
        erc20L2DataSource = erc20L2DataSource,
        ethLayerTwoFeatureFlag = ethLayerTwoFeatureFlag,
        evmWithoutL1BalanceFeatureFlag = evmWithoutL1BalanceFeatureFlag
    )

    @Test
    fun `accountHash fetches from eth data manager`() {
        val result = subject.accountHash

        assertEquals(ACCOUNT_HASH, result)

        io.mockk.verify { ethDataManager.accountAddress }
        verifyNoMoreInteractions(erc20StoreService)
        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `eth balance is fetched from eth data manager`() {

        val ethBalance = 1001.toBigInteger()

        coEvery { ethDataManager.getBalance() } returns Outcome.Success(ethBalance)
        every { ethDataManager.supportedNetworks } returns Single.just(setOf(EthDataManager.ethChain))
        every { assetCatalogue.fromNetworkTicker(EthDataManager.ethChain.networkTicker) } returns CryptoCurrency.ETHER

        val expectedResult = CryptoValue.fromMinor(CryptoCurrency.ETHER, ethBalance)
        val result = subject.getL1TokenBalance(CryptoCurrency.ETHER).blockingGet()
        assertEquals(expectedResult, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getErc20Balance fails for non-erc20 assets`() {
        subject.getErc20Balance(CryptoCurrency.ETHER)
    }

    @Test
    fun `getErc20Balance delegates to balance cache`() {
        val mockBalance: Erc20Balance = mock()
        whenever(erc20StoreService.getBalanceFor(asset = ERC20_TOKEN))
            .thenReturn(Observable.just(mockBalance))

        val result = subject.getErc20Balance(ERC20_TOKEN).blockingFirst()
        assertEquals(mockBalance, result)

        verify(erc20StoreService).getBalanceFor(asset = ERC20_TOKEN)

        verifyNoMoreInteractions(erc20StoreService)
    }

    @Test
    fun `getErc20Balance returns zero if asset not found`() {
        whenever(erc20StoreService.getBalanceFor(asset = UNKNOWN_ERC20_TOKEN))
            .thenReturn(Observable.just(Erc20Balance.zero(UNKNOWN_ERC20_TOKEN)))

        val result = subject.getErc20Balance(UNKNOWN_ERC20_TOKEN).blockingFirst()
        assert(result.balance.isZero)

        verify(erc20StoreService).getBalanceFor(asset = UNKNOWN_ERC20_TOKEN)

        verifyNoMoreInteractions(erc20StoreService)
    }

    @Test
    fun `getErc20History delegates to history cache`() {
        val mockEvent: Erc20HistoryEvent = mock()
        val mockEventList = listOf(mockEvent)

        whenever(historyCallCache.fetch(ACCOUNT_HASH, ERC20_TOKEN, EthDataManager.ethChain.networkTicker))
            .thenReturn(Single.just(mockEventList))

        subject.getErc20History(ERC20_TOKEN, EthDataManager.ethChain)
            .test()
            .assertValue(mockEventList)

        verify(historyCallCache).fetch(ACCOUNT_HASH, ERC20_TOKEN, EthDataManager.ethChain.networkTicker)
        io.mockk.verify { ethDataManager.accountAddress }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `createErc20Transaction correctly constructs a transaction without hot wallet`() {
        val nonce = 1001.toBigInteger()
        every { ethDataManager.getNonce(any()) } returns Single.just(nonce)

        val destination = ""
        val to = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val amount = 200.toBigInteger()
        val gasPrice = 5.toBigInteger()
        val gasLimit = 21.toBigInteger()

        val expectedPayload = "a9059cbb0000000000000000000000002ca28ffadd20474ffe2705580279a1e67cd10a29" +
            "00000000000000000000000000000000000000000000000000000000000000c8"

        subject.createErc20Transaction(
            asset = ERC20_TOKEN,
            to = to,
            amount = amount,
            gasPriceWei = gasPrice,
            gasLimitGwei = gasLimit,
            hotWalletAddress = destination
        ).test()
            .assertValue { raw ->
                raw.nonce == nonce &&
                    raw.gasPrice == gasPrice &&
                    raw.gasLimit == gasLimit &&
                    raw.to == CONTRACT_ADDRESS &&
                    raw.value == BigInteger.ZERO &&
                    raw.data == expectedPayload
            }

        io.mockk.verify { ethDataManager.getNonce(any()) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `createErc20Transaction correctly constructs a transaction using hot wallet`() {
        val nonce = 1001.toBigInteger()
        val extraGasLimit = 1.toBigInteger()
        every { ethDataManager.getNonce(any()) } returns Single.just(nonce)
        every { ethDataManager.extraGasLimitForMemo() } returns extraGasLimit

        val destination = "0x2ca28ffadd20474ffe2705580279a1e67cd10a29"
        val hotWalletAddress = "0x2ca28ffadd20474ffe2705580279a1e67cd10a30"
        val amount = 200.toBigInteger()
        val gasPrice = 5.toBigInteger()
        val gasLimit = 21.toBigInteger()

        val expectedPayload = "a9059cbb0000000000000000000000002ca28ffadd20474ffe2705580279a1e67cd10a30" +
            "00000000000000000000000000000000000000000000000000000000000000c8" +
            "0000000000000000000000002ca28ffadd20474ffe2705580279a1e67cd10a29"

        subject.createErc20Transaction(
            asset = ERC20_TOKEN,
            to = destination,
            amount = amount,
            gasPriceWei = gasPrice,
            gasLimitGwei = gasLimit,
            hotWalletAddress = hotWalletAddress
        ).test()
            .assertValue { raw ->
                raw.nonce == nonce &&
                    raw.gasPrice == gasPrice &&
                    raw.gasLimit == (gasLimit + extraGasLimit) &&
                    raw.to == CONTRACT_ADDRESS &&
                    raw.value == BigInteger.ZERO &&
                    raw.data == expectedPayload
            }

        io.mockk.verify { ethDataManager.getNonce(any()) }
        io.mockk.verify { ethDataManager.extraGasLimitForMemo() }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `signErc20Transaction delegates to eth data manager`() {
        val rawTx: RawTransaction = mock()
        val secondPassword = "SecondPassword"
        val result = "This Is The Signed tx bytes".toByteArray()

        every { ethDataManager.signEthTransaction(rawTx, secondPassword) } returns Single.just(result)

        subject.signErc20Transaction(rawTx, secondPassword, CryptoCurrency.ETHER.networkTicker)
            .test()
            .assertValue { it.contentEquals(result) }

        io.mockk.verify { ethDataManager.signEthTransaction(rawTx, secondPassword) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `pushErc20Transaction delegates to eth data manager`() {
        val signedBytes = "This Is The Signed tx bytes".toByteArray()
        every { ethDataManager.pushTx(signedBytes) } returns Single.just(TX_HASH)

        subject.pushErc20Transaction(signedBytes, CryptoCurrency.ETHER.networkTicker)
            .test()
            .assertValue { it == TX_HASH }

        io.mockk.verify { ethDataManager.pushTx(signedBytes) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getErc20TxNote fails in called for a non-erc20 token`() {
        subject.getErc20TxNote(CryptoCurrency.ETHER, TX_HASH)
    }

    @Test
    fun `getErc20TxNote delegates to eth data manager`() {
        val note = "This is a note"
        val notesMap: HashMap<String, String> = mock {
            on { get(TX_HASH) }.thenReturn(note)
        }

        val tokenData: Erc20TokenData = mock {
            on { txNotes }.thenReturn(notesMap)
        }
        every { ethDataManager.getErc20TokenData(ERC20_TOKEN) } returns tokenData

        val result = subject.getErc20TxNote(ERC20_TOKEN, TX_HASH)

        assertEquals(note, result)

        io.mockk.verify { ethDataManager.getErc20TokenData(ERC20_TOKEN) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `putErc20TxNote fails in called for a non-erc20 token`() {
        subject.putErc20TxNote(CryptoCurrency.ETHER, TX_HASH, "This is a note")
    }

    @Test
    fun `putErc20TxNote delegates to eth data manager`() {
        val note = "This is a note"

        every {
            ethDataManager.updateErc20TransactionNotes(
                ERC20_TOKEN,
                TX_HASH,
                note
            )
        } returns Completable.complete()

        subject.putErc20TxNote(ERC20_TOKEN, TX_HASH, note)
            .test()
            .assertComplete()

        io.mockk.verify { ethDataManager.updateErc20TransactionNotes(ERC20_TOKEN, TX_HASH, note) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `hasUnconfirmedTransactions delegates to eth data manager`() {
        every { ethDataManager.isLastTxPending() } returns Single.just(true)

        subject.hasUnconfirmedTransactions()
            .test()
            .assertValue { it == true }

        io.mockk.verify { ethDataManager.isLastTxPending() }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `latestBlockNumber delegates to eth data manager`() {
        val blockNumber = 19000.toBigInteger()

        val lastBlock: EthLatestBlockNumber = mock {
            on { number }.thenReturn(blockNumber)
        }
        every { ethDataManager.getLatestBlockNumber(any()) } returns Single.just(lastBlock)

        subject.latestBlockNumber(EthDataManager.ethChain.networkTicker)
            .test()
            .assertValue { it == blockNumber }

        io.mockk.verify { ethDataManager.getLatestBlockNumber(any()) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `isContractAddress delegates for contract address`() {
        every { ethDataManager.isContractAddress(CONTRACT_ADDRESS) } returns Single.just(true)

        subject.isContractAddress(CONTRACT_ADDRESS)
            .test()
            .assertValue(true)

        io.mockk.verify { ethDataManager.isContractAddress(CONTRACT_ADDRESS) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test
    fun `isContractAddress delegates for non-contract address`() {
        every { ethDataManager.isContractAddress(ACCOUNT_HASH) } returns Single.just(false)

        subject.isContractAddress(ACCOUNT_HASH)
            .test()
            .assertValue(false)

        io.mockk.verify { ethDataManager.isContractAddress(ACCOUNT_HASH) }

        verifyNoMoreInteractions(historyCallCache)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `flushCaches fails in called for a non-erc20 token`() {
        subject.flushCaches(CryptoCurrency.ETHER)
    }

    @Test
    fun `flushCaches clears cached API data`() {
        subject.flushCaches(ERC20_TOKEN)

        verify(erc20DataSource).invalidate()
        verify(erc20L2DataSource).invalidate(ERC20_TOKEN.networkTicker)

        verify(historyCallCache).flush(ERC20_TOKEN)

        verifyNoMoreInteractions(historyCallCache)
    }

    companion object {
        const val ACCOUNT_HASH = "0x4058a004dd718babab47e14dd0d744742e5b9903"
        const val CONTRACT_ADDRESS = "0x8e870d67f660d95d5be530380d0ec0bd388289e1"

        const val TX_HASH = "0xfd7d583fa54bf55f6cfbfec97c0c55cc6af8c121b71addb7d06a9e1e305ae8ff"

        private val ERC20_TOKEN: AssetInfo = object : CryptoCurrency(
            displayTicker = "DUMMY",
            networkTicker = "DUMMY",
            name = "Dummies",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            l1chainTicker = ETHER.networkTicker,
            l2identifier = CONTRACT_ADDRESS,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val UNKNOWN_ERC20_TOKEN: AssetInfo = object : CryptoCurrency(
            displayTicker = "WHATEVER",
            networkTicker = "WHATEVER",
            name = "Whatevs",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            l1chainTicker = ETHER.networkTicker,
            l2identifier = CONTRACT_ADDRESS,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}
    }
}
