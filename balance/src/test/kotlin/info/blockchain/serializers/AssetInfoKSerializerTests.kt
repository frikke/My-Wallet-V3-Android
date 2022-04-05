package info.blockchain.serializers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class AssetInfoKSerializerTests {

    private val assetCatalogue: AssetCatalogue = mock()

    private val expectedSerialJson = "{\"value\":\"BTC\"}"

    @Serializable
    private data class TestClass(
        val value: @Contextual AssetInfo
    )

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(AssetInfoKSerializer(assetCatalogue = assetCatalogue))
        }
    }

    @Before
    fun setUp() {
        whenever(assetCatalogue.assetInfoFromNetworkTicker(any())).thenReturn(CryptoCurrency.BTC)
    }

    @Test
    fun `AssetInfoKSerializer serialisation`() {
        val jsonResponse = json.encodeToString(TestClass(CryptoCurrency.BTC))
        jsonResponse shouldBeEqualTo expectedSerialJson
    }

    @Test
    fun `AssetInfoKSerializer deSerialisation`() {
        val testClass = json.decodeFromString<TestClass>(expectedSerialJson)
        testClass shouldBeEqualTo TestClass(CryptoCurrency.BTC)
    }
}
