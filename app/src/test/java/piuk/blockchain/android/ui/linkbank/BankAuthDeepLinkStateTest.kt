package piuk.blockchain.android.ui.linkbank

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.BankPartner
import com.blockchain.domain.paymentmethods.model.LinkedBank
import com.blockchain.domain.paymentmethods.model.LinkedBankErrorState
import com.blockchain.domain.paymentmethods.model.LinkedBankState
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.replaceGsonKtxFeatureFlag
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.testutils.GBP
import com.blockchain.testutils.eur
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class BankAuthDeepLinkStateTest : KoinTest {

    private val replaceGsonKtxFF = mockk<FeatureFlag>()

    private val jsonSerializers = module {
        single {
            Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                serializersModule = SerializersModule {
                    contextual(BigDecimalSerializer)
                }
            }
        }
    }

    private val featureFlagsModule = module {
        single(replaceGsonKtxFeatureFlag) {
            replaceGsonKtxFF
        }
    }

    private val state = BankAuthDeepLinkState(
        bankPaymentData = BankPaymentApproval(
            paymentId = "txId",
            authorisationUrl = "authUrl",
            linkedBank = LinkedBank(
                id = "id",
                currency = GBP,
                partner = BankPartner.YAPILY,
                accountName = "name",
                bankName = "bankName",
                accountNumber = "123",
                state = LinkedBankState.BLOCKED,
                errorStatus = LinkedBankErrorState.ACCOUNT_ALREADY_LINKED,
                accountType = "",
                authorisationUrl = "url",
                sortCode = "123",
                accountIban = "123",
                bic = "123",
                entity = "entity",
                iconUrl = "iconUrl",
                callbackPath = ""
            ),
            orderValue = 10.eur()
        ),
        bankLinkingInfo = BankLinkingInfo(
            linkingId = "id",
            bankAuthSource = BankAuthSource.SIMPLE_BUY
        )
    )

    private val ktxString =
        """{"bankAuthFlow":"NONE","bankPaymentData":{"paymentId":"txId","authorisationUrl":"authUrl","linkedBank":{"id":"id","currency":{"currencyCode":"GBP"},"partner":"YAPILY","bankName":"bankName","accountName":"name","accountNumber":"123","state":"BLOCKED","errorStatus":"ACCOUNT_ALREADY_LINKED","accountType":"","authorisationUrl":"url","sortCode":"123","accountIban":"123","bic":"123","entity":"entity","iconUrl":"iconUrl","callbackPath":""},"orderValue":{"currency":{"currencyCode":"EUR"},"amount":"10.00","symbol":"€"}},"bankLinkingInfo":{"linkingId":"id","bankAuthSource":"SIMPLE_BUY"}}"""
    private val gsonString =
        """{"bankAuthFlow":"NONE","bankPaymentData":{"paymentId":"txId","authorisationUrl":"authUrl","linkedBank":{"id":"id","currency":{"currencyCode":"GBP"},"partner":"YAPILY","bankName":"bankName","accountName":"name","accountNumber":"123","state":"BLOCKED","errorStatus":"ACCOUNT_ALREADY_LINKED","accountType":"","authorisationUrl":"url","sortCode":"123","accountIban":"123","bic":"123","entity":"entity","iconUrl":"iconUrl","callbackPath":""},"orderValue":{"currency":{"currencyCode":"EUR"},"amount":10.00,"symbol":"€"}},"bankLinkingInfo":{"linkingId":"id","bankAuthSource":"SIMPLE_BUY"}}"""

    @Before
    fun setUp() {
        startKoin {
            modules(
                jsonSerializers,
                featureFlagsModule
            )
        }
    }

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `GIVEN ktx enabled, WHEN toPreferencesValue is called, THEN ktxString should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns true

        val result = state.toPreferencesValue()

        assertEquals(ktxString, result)
    }

    @Test
    fun `GIVEN ktx enabled, WHEN fromPreferencesValue is called, THEN state should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns false

        val result = ktxString.fromPreferencesValue()

        assertEquals(state, result)
    }

    @Test
    fun `GIVEN gson enabled, WHEN toPreferencesValue is called, THEN gsonString should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns false

        val result = state.toPreferencesValue()

        assertEquals(gsonString, result)
    }

    @Test
    fun `GIVEN gson enabled, WHEN fromPreferencesValue is called, THEN state should be returned`() {
        every { replaceGsonKtxFF.isEnabled } returns false

        val result = gsonString.fromPreferencesValue()

        assertEquals(state, result)
    }
}
