package com.blockchain.metadata

import com.blockchain.serialization.JsonSerializable
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import java.math.BigDecimal
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.mockito.Mockito

@InternalSerializationApi
class MetadataRepositoryAdapterTest : KoinTest {

    private val json = Json {
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
        }
    }

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Serializable
    data class ExampleClass(
        val field1: String,
        @Serializable(with = BigDecimalSerializer::class)
        val field2: BigDecimal
    ) : JsonSerializable

    @Test
    fun `can save json`() {
        val metadataManager = mock<MetadataManager> {
            on { saveToMetadata(any(), any()) }.thenReturn(Completable.complete())
        }
        val randomEntry = MetadataEntry.values().random()
        MetadataRepositoryAdapter(metadataManager, json)
            .save(
                ExampleClass("ABC", 123.toBigDecimal()),
                randomEntry
            )
            .test()
            .assertComplete()

        verify(metadataManager).saveToMetadata("""{"field1":"ABC","field2":"123"}""", randomEntry.index)
    }

    @Test
    fun `can load json`() {
        val randomEntry = MetadataEntry.values().random()
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(randomEntry.index) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","field2":"456"}"""
                )
            )
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(randomEntry, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf(ExampleClass("DEF", 456.toBigDecimal()))
    }

    @Test
    fun `can load missing json`() {
        val randomEntry = MetadataEntry.values().random()
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(randomEntry.index) }.thenReturn(Maybe.empty())
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(randomEntry, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf()
    }

    @Test
    fun `bad json is an error`() {
        val randomEntry = MetadataEntry.values().random()
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(randomEntry.index) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","fie..."""
                )
            )
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(randomEntry, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `missing fields should not get serialised`() {
        val metadataManager: MetadataManager = mock {
            on { saveToMetadata(any(), any()) }.thenReturn(Completable.complete())
        }
        val randomEntry = MetadataEntry.values().random()
        val metadataRepository = MetadataRepositoryAdapter(metadataManager, json)

        val test = metadataRepository.save(
            TestObjectWithNullableProperties(
                userId = "UserId",
                lifetimeToken = "lifetimetoken"
            ),
            randomEntry
        ).test()

        test.assertComplete()
        Mockito.verify(metadataManager)
            .saveToMetadata(
                "{\"userId\":\"UserId\",\"lifetimeToken\":\"lifetimetoken\"}",
                randomEntry.index
            )
    }
}

@Serializable
data class TestObjectWithNullableProperties(
    private val userId: String,
    private val lifetimeToken: String,
    private val nullableProperty: String? = null
) : JsonSerializable
