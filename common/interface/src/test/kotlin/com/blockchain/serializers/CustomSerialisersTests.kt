package com.blockchain.serializers

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import java.math.BigDecimal
import java.util.Date
import java.util.GregorianCalendar
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class BigDecimalJsonTests {

    @Serializable
    private class TestClass(
        val value: @Contextual BigDecimal
    )

    private val jsonAdapter = Json {
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            isLenient = true
        }
    }

    @Test
    fun `json to big decimal`() {
        jsonAdapter.decodeFromString<TestClass>(
            """
               {
                 "value": 0.3
                }
            """
        ).value `should be equal to` "0.3".toBigDecimal()
    }

    @Test
    fun `json to big decimal (string)`() {
        jsonAdapter.decodeFromString<TestClass>(
            """
               {
                 "value": "0.3"
                }
            """
        ).value `should be equal to` "0.3".toBigDecimal()
    }

    @Test
    fun `big decimal to json`() {
        jsonAdapter.encodeToString(
            TestClass("0.3".toBigDecimal())
        ) `should be equal to` """{"value":"0.3"}"""
    }

    @Test
    fun `18 digit big decimal to json`() {
        jsonAdapter.encodeToString(
            TestClass("0.123456789012345678".toBigDecimal())
        ) `should be equal to` """{"value":"0.123456789012345678"}"""
    }

    @Test
    fun `large btc example big decimal to json`() {
        jsonAdapter.encodeToString(
            TestClass(12345678.12345678.bitcoin().toBigDecimal())
        ) `should be equal to` """{"value":"12345678.12345678"}"""
    }

    @Test
    fun `large ether example big decimal to json`() {
        jsonAdapter.encodeToString(
            TestClass(12345678.12345678.ether().toBigDecimal())
        ) `should be equal to` """{"value":"12345678.123456780000000000"}"""
    }

    @Test
    fun `very large big decimal to json`() {
        jsonAdapter.encodeToString(
            TestClass("123456789012345678901".toBigDecimal())
        ) `should be equal to` """{"value":"123456789012345678901"}"""
    }
}

class StringMapSerializerTest {

    @Serializable
    data class TestClass(
        val map: @Contextual Map<String, String>
    )

    private val json = Json {
        serializersModule = SerializersModule { contextual(StringMapSerializer) }
    }

    private val containerClass = TestClass(mapOf("key" to "value"))
    private val jsonString = "{\"map\":{\"key\":\"value\"}}"

    @Test
    fun `stringMap serialisation`() {
        json.encodeToString(containerClass) shouldBeEqualTo jsonString
    }

    @Test
    fun `stringMap deSerialisation`() {
        json.decodeFromString<TestClass>(jsonString) shouldBeEqualTo containerClass
    }
}

class PrimitiveSerializerTest {

    @Serializable
    data class TestClass(
        val number: @Contextual Any,
        val text: @Contextual Any,
        val bool: @Contextual Any
    )

    private val json = Json {
        serializersModule = SerializersModule { contextual(PrimitiveSerializer) }
    }

    private val containerClass = TestClass(12, "lorem", true)
    private val jsonString = "{\"number\":12,\"text\":\"lorem\",\"bool\":true}"

    @Test
    fun `primitive serialisation`() {
        json.encodeToString(containerClass) shouldBeEqualTo jsonString
    }

    @Test
    fun `primitive deSerialisation`() {
        val result = json.decodeFromString<TestClass>(jsonString)

        result.number.toString() shouldBeEqualTo containerClass.number.toString()
        result.text.toString() shouldBeEqualTo "\"${containerClass.text}\""
        result.bool.toString() shouldBeEqualTo containerClass.bool.toString()
    }
}

class IsoDateSerializerTest {

    @Serializable
    data class TestClass(
        val date: @Contextual Date
    )

    private val json = Json {
        serializersModule = SerializersModule { contextual(IsoDateSerializer) }
    }

    private val date = GregorianCalendar(2022, 11, 18, 14, 22, 44).time
    private val jsonString = "{\"date\":\"2022-12-18T14:22:44\"}"

    @Test
    fun `date serialisation`() {
        json.encodeToString(TestClass(date)) shouldBeEqualTo jsonString
    }

    @Test
    fun `date deSerialisation`() {
        json.decodeFromString<TestClass>(jsonString) shouldBeEqualTo TestClass(date)
    }
}
