package piuk.blockchain.android.ui.activity.detail

import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialActivitySummaryItem
import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.testutils.satoshi
import com.blockchain.testutils.satoshiCash
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.payload.data.Wallet
import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import kotlin.test.assertEquals

@Suppress("PrivatePropertyName")
class TransactionHelperTest {
    private val payloadDataManager: PayloadDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val payload: Wallet = mock()

    private val cryptoValBtc_1 = 1.satoshi()
    private val cryptoValBtc_15 = 15.satoshi()
    private val cryptoValBch_1 = 1.satoshiCash()
    private val cryptoValBch_15 = 15.satoshiCash()

    private val subject: TransactionHelper =
        TransactionHelper(
            payloadDataManager,
            bchDataManager
        )

    @Test
    fun filterNonChangeAddressesSingleInput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            exchangeRates = mock(),
            transactionType = TransactionSummary.TransactionType.RECEIVED,
            inputsMap = mapOf(
                "key" to cryptoValBtc_1
            )
        )

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeReceivedAddressesMultipleInput() { // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.RECEIVED,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1
            )
        )

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesMultipleInput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1,
                "key2" to cryptoValBtc_1
            )
        )

        whenever(payloadDataManager.getXpubFromAddress("key0"))
            .thenReturn("xpub")
        whenever(payloadDataManager.getXpubFromAddress("key1"))
            .thenReturn("xpub")

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(2, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key" to cryptoValBtc_1
            )
        )

        whenever(payload.importedAddressStringList)
            .thenReturn(emptyList())
        whenever(payloadDataManager.wallet)
            .thenReturn(payload)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputMultipleOutput() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBtc_1,
                "key1" to cryptoValBtc_1,
                "key2" to cryptoValBtc_15
            ),
            value = 10.satoshi()
        )

        val importedStrings = listOf("key0", "key1")

        whenever(payload.importedAddressStringList)
            .thenReturn(importedStrings)

        whenever(payloadDataManager.wallet)
            .thenReturn(payload)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(2, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputHD() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBtc_1
            ),
            value = 10.satoshi()
        )

        val importedStrings = listOf("key0", "key1")

        whenever(payload.importedAddressStringList)
            .thenReturn(importedStrings)

        whenever(payloadDataManager.wallet)
            .thenReturn(payload)
        whenever(payloadDataManager.isOwnHDAddress(any()))
            .thenReturn(true)

        // Act
        val value = subject.filterNonChangeBtcAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesMultipleInputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            asset = CryptoCurrency.BCH,
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1,
                "key1" to cryptoValBch_1,
                "key2" to cryptoValBch_1
            )
        )

        whenever(bchDataManager.getXpubFromAddress("key0"))
            .thenReturn("xpub")
        whenever(bchDataManager.getXpubFromAddress("key1"))
            .thenReturn("xpub")

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(2, value.left.size)
        assertEquals(0, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            asset = CryptoCurrency.BCH,
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key" to cryptoValBch_1
            )
        )

        whenever(bchDataManager.getImportedAddressStringList())
            .thenReturn(emptyList())

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(1, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputMultipleOutputBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBch_1,
                "key1" to cryptoValBch_1,
                "key2" to cryptoValBch_15
            ),
            value = 10.satoshiCash()
        )

        val importedStrings = listOf("key0", "key1")

        whenever(bchDataManager.getImportedAddressStringList())
            .thenReturn(importedStrings)

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.left.size)
        assertEquals(2, value.right.size)
    }

    @Test
    fun filterNonChangeAddressesSingleInputSingleOutputHDBch() {
        // Arrange
        val item = TestNonCustodialSummaryItem(
            transactionType = TransactionSummary.TransactionType.SENT,
            inputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            outputsMap = mapOf(
                "key0" to cryptoValBch_1
            ),
            value = 10.satoshi()
        )

        val importedStrings = listOf("key0", "key1")

        whenever(bchDataManager.getImportedAddressStringList())
            .thenReturn(importedStrings)

        whenever(bchDataManager.isOwnAddress(any()))
            .thenReturn(true)

        // Act
        val value = subject.filterNonChangeBchAddresses(item)

        // Assert
        assertEquals(1, value.right.size)
        assertEquals(1, value.left.size)
    }
}

class TestNonCustodialSummaryItem(
    override val exchangeRates: ExchangeRatesDataManager = mock(),
    override val asset: AssetInfo = CryptoCurrency.BTC,
    override val transactionType: TransactionSummary.TransactionType = TransactionSummary.TransactionType.RECEIVED,
    override val timeStampMs: Long = 0,
    override val value: CryptoValue = CryptoValue.zero(CryptoCurrency.BTC),
    override val fee: Observable<CryptoValue> = Observable.just(CryptoValue.zero(CryptoCurrency.BTC)),
    override val txId: String = "",
    override val inputsMap: Map<String, CryptoValue> = emptyMap(),
    override val outputsMap: Map<String, CryptoValue> = emptyMap(),
    override val description: String? = null,
    override val confirmations: Int = 0,
    override val isFeeTransaction: Boolean = false,
    override val account: CryptoAccount = mock(),
    override val supportsDescription: Boolean = true
) : NonCustodialActivitySummaryItem()
