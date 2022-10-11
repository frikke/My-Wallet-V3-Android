package piuk.blockchain.androidcore.data.ethereum

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.NonCustodialEvmService
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.chains.ethereum.datastores.EthDataStore
import com.blockchain.logging.LastTxUpdater
import com.blockchain.metadata.MetadataRepository
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthAccountDto
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.EthereumWalletData
import info.blockchain.wallet.ethereum.EthereumWalletDto
import info.blockchain.wallet.ethereum.data.EthLatestBlockNumber
import info.blockchain.wallet.ethereum.data.EthTransaction
import info.blockchain.wallet.ethereum.node.EthJsonRpcRequest
import info.blockchain.wallet.ethereum.node.EthJsonRpcResponse
import info.blockchain.wallet.ethereum.node.RequestType
import info.blockchain.wallet.keys.MasterKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class EthDataManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private val payloadManager: PayloadDataManager = mock()
    private val ethAccountApi: EthAccountApi = mockk()
    private val ethDataStore: EthDataStore = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
    private val metadataRepository: MetadataRepository = mock()
    private val lastTxUpdater: LastTxUpdater = mock()
    private val evmNetworksService: EvmNetworksService = mock {
        on { getSupportedNetworks() }.thenReturn(Single.just(emptyList()))
    }
    private val nonCustodialEvmService: NonCustodialEvmService = mock()

    private val subject = EthDataManager(
        payloadDataManager = payloadManager,
        ethAccountApi = ethAccountApi,
        ethDataStore = ethDataStore,
        metadataRepository = metadataRepository,
        lastTxUpdater = lastTxUpdater,
        evmNetworksService = evmNetworksService,
        nonCustodialEvmService = nonCustodialEvmService
    )

    @Test
    fun clearEthAccountDetails() {
        // Arrange

        // Act
        subject.clearAccountDetails()

        // Assert
        verify(ethDataStore).clearData()
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun fetchEthAddress() {
        // Arrange
        val ethAddress = "ADDRESS"
        whenever(ethDataStore.ethWallet!!.account.address).thenReturn(ethAddress)

        every { ethAccountApi.getEthAddress(listOf(ethAddress)) } returns Observable.just(hashMapOf())
        // Act
        val testObserver = subject.fetchEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verify(ethDataStore).ethAddressResponse = any()
        verifyZeroInteractions(ethDataStore)
        io.mockk.verify { ethAccountApi.getEthAddress(listOf(ethAddress)) }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get balance found`() {
        runTest {
            // Arrange
            val ethAddress = "ADDRESS"
            val requestType = RequestType.GET_BALANCE
            val response: EthJsonRpcResponse = mock {
                on { result }.thenReturn("0xA")
            }
            val expected = Outcome.Success(BigInteger.TEN)
            whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
            coEvery {
                ethAccountApi.postEthNodeRequest(any(), requestType, ethAddress, EthJsonRpcRequest.defaultBlock)
            } returns Outcome.Success(response)

            // Act
            val result = subject.getBalance()

            // Assert
            result `should be equal to` expected
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `get balance error, still returns value`() {
        runTest {
            // Arrange
            val ethAddress = "ADDRESS"
            val requestType = RequestType.GET_BALANCE
            val errorResponse = Outcome.Failure(Exception())
            whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
            coEvery {
                ethAccountApi.postEthNodeRequest(any(), requestType, ethAddress, EthJsonRpcRequest.defaultBlock)
            } returns errorResponse

            // Act
            val result = subject.getBalance()
            // Assert
            result `should be equal to` errorResponse
        }
    }

    @Test
    fun getEthResponseModel() {
        // Arrange

        // Act
        subject.getEthResponseModel()
        // Assert
        verify(ethDataStore).ethAddressResponse
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun `getEthTransactions response found with 3 transactions`() {
        // Arrange
        val ethAddress = "ADDRESS"
        val ethTransaction: EthTransaction = mock()
        whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
        every { ethAccountApi.getEthTransactions(listOf(ethAddress)) } returns
            Single.just(listOf(ethTransaction, ethTransaction, ethTransaction))
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        val values = testObserver.values()
        values[0] `should contain` ethTransaction

        values.size `should be equal to` 1
    }

    @Test
    fun `getEthTransactions response not found`() {
        // Arrange
        whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(null)
        // Act
        val testObserver = subject.getEthTransactions().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, emptyList())
    }

    @Test
    fun `lastTx is pending when there is at least one transaction pending`() {
        // Arrange
        val ethAddress = "Address"
        whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
        every { ethAccountApi.getLastEthTransaction(listOf(ethAddress)) } returns
            Maybe.just(EthTransaction(state = "PENDING"))
        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, true)
    }

    @Test
    fun `lastTx is not pending when there is no pending tx`() {
        // Arrange
        val ethAddress = "Address"
        whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
        every { ethAccountApi.getLastEthTransaction(listOf(ethAddress)) } returns
            Maybe.just(EthTransaction(state = "CONFIRMED"))
        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, false)
    }

    @Test
    fun `lastTx is not pending when there is no tx`() {
        // Arrange
        val ethAddress = "Address"
        whenever(ethDataStore.ethWallet!!.account!!.address).thenReturn(ethAddress)
        every { ethAccountApi.getLastEthTransaction(listOf(ethAddress)) } returns Maybe.empty()

        // Act
        val result = subject.isLastTxPending().test()
        // Assert

        result.assertValueCount(1)
        result.assertValueAt(0, false)
    }

    @Test
    fun getLatestBlock() {
        // Arrange
        val latestBlock = EthLatestBlockNumber()
        coEvery { ethAccountApi.getLatestBlockNumber(any()) } returns Outcome.Success(latestBlock)
        // Act
        val testObserver = subject.getLatestBlockNumber().test().await()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(latestBlock)
        coVerify { ethAccountApi.getLatestBlockNumber(any()) }
    }

    @Test
    fun getIfContract() {
        // Arrange
        val address = "0xThisIsAValidEthAddress"
        val requestType = RequestType.IS_CONTRACT
        val response: EthJsonRpcResponse = mockk {
            coEvery { result } returns "contract"
        }
        coEvery {
            ethAccountApi.postEthNodeRequest(any(), requestType, address, EthJsonRpcRequest.defaultBlock)
        }.returns(Outcome.Success(response))
        // Act
        val result = subject.isContractAddress(address).blockingGet()
        // Assert
        result `should be equal to` true
        coVerify { ethAccountApi.postEthNodeRequest(any(), requestType, address, EthJsonRpcRequest.defaultBlock) }
    }

    @Test
    fun `getTransactionNotes returns string object`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet!!.getTxNotes()[hash]).thenReturn(notes)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should be equal to` notes
    }

    @Test
    fun `getTransactionNotes returns null object as wallet is missing`() {
        // Arrange
        val hash = "HASH"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val result = subject.getTransactionNotes(hash)
        // Assert
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        result `should be equal to` null
    }

    @Test
    fun `updateTransactionNotes success`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        val ethereumWallet = EthereumWallet(
            EthereumWalletDto(
                EthereumWalletData(
                    _hasSeen = true,
                    _txNotes = emptyMap(),
                    _accounts = listOf(
                        EthAccountDto(
                            _archived = false,
                            label = "123",
                            _isCorrect = true,
                            address = "23"
                        )
                    )
                )
            )
        )
        whenever(ethDataStore.ethWallet).thenReturn(ethereumWallet)
        whenever(metadataRepository.saveRawValue(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
        verify(metadataRepository).saveRawValue(any(), any())
        verifyNoMoreInteractions(metadataRepository)
    }

    @Test
    fun `updateTransactionNotes wallet not found`() {
        // Arrange
        val hash = "HASH"
        val notes = "NOTES"
        whenever(ethDataStore.ethWallet).thenReturn(null)
        // Act
        val testObserver = subject.updateTransactionNotes(hash, notes).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(NullPointerException::class.java)
        verify(ethDataStore).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun signEthTransaction() {
        // Arrange
        val rawTransaction: RawTransaction = mock()
        val byteArray = ByteArray(32)
        val masterKey: MasterKey = mock()

        whenever(
            ethDataStore.ethWallet!!.account.signTransaction(
                eq(rawTransaction),
                eq(masterKey),
                eq(EthDataManager.ETH_CHAIN_ID)
            )
        ).thenReturn(byteArray)

        whenever(payloadManager.masterKey).thenReturn(masterKey)

        // Act
        val testObserver = subject.signEthTransaction(rawTransaction, "").test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(byteArray)
        verify(ethDataStore, atLeastOnce()).ethWallet
        verifyNoMoreInteractions(ethDataStore)
    }

    @Test
    fun pushEthTx() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"
        coEvery { ethAccountApi.pushTx(any()) } returns Single.just(hash)
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.complete())
        // Act
        val result = subject.pushTx(byteArray).blockingGet()
        // Assert
        result `should be equal to` hash
        coVerify { ethAccountApi.pushTx(any()) }
    }

    @Test
    fun `pushEthTx returns hash despite update last tx failing`() {
        // Arrange
        val byteArray = ByteArray(32)
        val hash = "HASH"

        coEvery { ethAccountApi.pushTx(any()) } returns Single.just(hash)
        whenever(lastTxUpdater.updateLastTxTime()).thenReturn(Completable.error(Exception()))

        // Act
        val result = subject.pushTx(byteArray).blockingGet()

        // Assert
        result `should be equal to` hash
        coVerify { ethAccountApi.pushTx(any()) }
    }
}
