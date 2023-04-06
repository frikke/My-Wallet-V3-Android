package piuk.blockchain.android.simplebuy

import android.content.res.Resources
import com.blockchain.nabu.datamanagers.BankDetail
import com.blockchain.nabu.models.responses.simplebuy.CustodialAccountAgentResponse
import com.blockchain.nabu.models.responses.simplebuy.CustodialAccountResponse
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R

class EURPaymentAccountMapperTest {

    private val resources: Resources = mock()
    private val bankAccountResponse: CustodialAccountResponse = mock()
    private val agent: CustodialAccountAgentResponse = mock()

    private lateinit var subject: EURPaymentAccountMapper

    @Before
    fun setUp() {
        mockResources()
        mockDefaultResponse()

        subject = EURPaymentAccountMapper(resources)
    }

    @Test
    fun `map non-EUR account`() {
        // Arrange
        whenever(bankAccountResponse.currency).thenReturn("USD")

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertNull(result)
    }

    @Test
    fun `map valid response`() {
        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertEquals(
            listOf(
                BankDetail(SWIFT, ACCOUNT, true),
                BankDetail(BANK_NAME, NAME, true),
                BankDetail(BANK_COUNTRY, COUNTRY),
                BankDetail(IBAN, "EE12 3456 0000 0000 0000", true),
                BankDetail(RECIPIENT_NAME, RECIPIENT)
            ),
            result?.details
        )
    }

    @Test
    fun `map missing account`() {
        // Arrange
        whenever(agent.account).thenReturn(null)

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertTrue(result?.details?.contains(BankDetail(SWIFT, "LHVBEE22", true)) ?: false)
    }

    @Test
    fun `map missing bank name`() {
        // Arrange
        whenever(agent.name).thenReturn(null)

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertNull(result)
    }

    @Test
    fun `map missing country`() {
        // Arrange
        whenever(agent.country).thenReturn(null)

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertTrue(result?.details?.contains(BankDetail(BANK_COUNTRY, ESTONIA)) ?: false)
    }

    @Test
    fun `map missing IBAN`() {
        // Arrange
        whenever(bankAccountResponse.address).thenReturn(null)

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertNull(result)
    }

    @Test
    fun `map missing recipient`() {
        // Arrange
        whenever(agent.recipient).thenReturn(null)

        // Act
        val result = subject.map(bankAccountResponse)

        // Assert
        assertTrue(result?.details?.contains(BankDetail(RECIPIENT_NAME, "")) ?: false)
    }

    private fun mockResources() {
        whenever(resources.getString(R.string.bank_code_swift_bic)).thenReturn(SWIFT)
        whenever(resources.getString(R.string.bank_name)).thenReturn(BANK_NAME)
        whenever(resources.getString(R.string.bank_country)).thenReturn(BANK_COUNTRY)
        whenever(resources.getString(R.string.estonia)).thenReturn(ESTONIA)
        whenever(resources.getString(R.string.iban)).thenReturn(IBAN)
        whenever(resources.getString(R.string.recipient_name)).thenReturn(RECIPIENT_NAME)
    }

    private fun mockDefaultResponse() {
        whenever(agent.account).thenReturn(ACCOUNT)
        whenever(agent.name).thenReturn(NAME)
        whenever(agent.country).thenReturn(COUNTRY)
        whenever(agent.recipient).thenReturn(RECIPIENT)

        whenever(bankAccountResponse.agent).thenReturn(agent)
        whenever(bankAccountResponse.currency).thenReturn(EUR)
        whenever(bankAccountResponse.address).thenReturn(ADDRESS)
    }

    private companion object {
        private const val SWIFT = "swift"
        private const val BANK_NAME = "bank name"
        private const val BANK_COUNTRY = "bank country"
        private const val ESTONIA = "estonia"
        private const val IBAN = "iban"
        private const val RECIPIENT_NAME = "recipient name"

        private const val EUR = "EUR"
        private const val ACCOUNT = "account"
        private const val NAME = "name"
        private const val COUNTRY = "country"
        private const val ADDRESS = "EE123456000000000000"
        private const val RECIPIENT = "recipient"
    }
}
