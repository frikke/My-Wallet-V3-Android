package com.blockchain.sunriver.datamanager

import com.blockchain.serialization.JsonSerializable
import com.blockchain.serialization.fromJson
import com.blockchain.serialization.toJson
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.`should be assignable from`
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class XlmMetaDataSerializationTest {

    private val json = Json {
        explicitNulls = false
    }

    @Test
    fun `XlmAccount is JsonSerializable`() {
        JsonSerializable::class `should be assignable from` XlmAccount::class
    }

    @Test
    fun `XlmMetadata is JsonSerializable`() {
        JsonSerializable::class `should be assignable from` XlmMetaData::class
    }

    @Test
    fun `can deserialize`() {
        XlmMetaData::class.fromJson(getStringFromResource("metadata/xlm_metadata.json"), json)
            .apply {
                defaultAccountIndex `should be` 0
                accounts `should be equal to` listOf(
                    XlmAccount(
                        "GBCRNVZPJFDBF3JECAXOTD2LTAQMLHVYJUV5IEGYJ5H73TA3EOW7RZJY",
                        _label = "My Stellar Wallet",
                        _archived = false
                    )
                )
                transactionNotes `should be equal to` emptyMap<String, String>()
            }
    }

    @Test
    fun `can deserialize alternative values`() {
        XlmMetaData::class.fromJson(getStringFromResource("metadata/xlm_metadata_2.json"), json)
            .apply {
                defaultAccountIndex `should be` 1
                accounts `should be equal to` listOf(
                    XlmAccount(
                        "GBNPUQCB2UY7YXBKZZYMRXDH3WMVD6XOGOHAU5U4WIXOPHKN3TRBXD2Z",
                        _label = "My Old Stellar Wallet",
                        _archived = true
                    ),
                    XlmAccount(
                        "GDTDFKFRZHTSGQGCSRWLJWCTR5BPM6LBLMQQ75G3DR4DANLDY73CTNU4",
                        _label = "My New Stellar Wallet",
                        _archived = false
                    )
                )
                transactionNotes `should be equal to` mapOf(
                    "tx1" to "something",
                    "tx2" to "something else"
                )
            }
    }

    @Test
    fun `can deserialize missing values`() {
        XlmMetaData::class.fromJson(getStringFromResource("metadata/xlm_metadata_with_missing_values.json"), json)
            .apply {
                defaultAccountIndex `should be` 0
                accounts `should be equal to` listOf(
                    XlmAccount(
                        publicKey = "GBNPUQCB2UY7YXBKZZYMRXDH3WMVD6XOGOHAU5U4WIXOPHKN3TRBXD2Z",
                        _label = null,
                        pubKey = null,
                        _archived = null
                    ),
                    XlmAccount(
                        publicKey = "GDTDFKFRZHTSGQGCSRWLJWCTR5BPM6LBLMQQ75G3DR4DANLDY73CTNU4",
                        _label = null,
                        pubKey = null,
                        _archived = null
                    )
                )
                transactionNotes `should be` null
            }
    }

    @Test
    fun `can deserialize missing everything`() {
        XlmMetaData::class.fromJson(input = "{}", json)
            .apply {
                defaultAccountIndex `should be` 0
                accounts `should be` null
                transactionNotes `should be` null
            }
    }

    @Test
    fun `can round trip`() {
        getStringFromResource("metadata/xlm_metadata.json")
            .assertJsonRoundTrip()
    }

    @Test
    fun `can round trip alternative values`() {
        getStringFromResource("metadata/xlm_metadata_2.json")
            .assertJsonRoundTrip()
    }

    @Test
    fun `can round trip with missing`() {
        getStringFromResource("metadata/xlm_metadata_with_missing_values.json")
            .assertJsonRoundTrip()
    }

    @Test
    fun `can round trip missing everything`() {
        "{}".assertJsonRoundTrip()
    }

    private fun String.assertJsonRoundTrip() {
        XlmMetaData::class.fromJson(this, json).assertJsonRoundTrip()
    }

    @OptIn(InternalSerializationApi::class)
    private fun XlmMetaData.assertJsonRoundTrip() {
        XlmMetaData::class.fromJson(toJson(XlmMetaData::class.serializer()), json) `should be equal to` this
    }
}
