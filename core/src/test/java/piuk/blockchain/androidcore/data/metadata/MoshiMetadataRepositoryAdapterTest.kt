package piuk.blockchain.androidcore.data.metadata

import com.blockchain.android.testutils.rxInit
import com.blockchain.metadata.save
import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.serialization.JsonSerializable
import com.blockchain.serializers.BigDecimalSerializer
import com.blockchain.serializers.BigIntSerializer
import com.blockchain.serializers.IsoDateSerializer
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
import org.mockito.Mockito

@InternalSerializationApi
class MetadataRepositoryAdapterTest {

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

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntSerializer)
            contextual(IsoDateSerializer)
        }
    }

    @Test
    fun `can save json`() {
        val metadataManager = mock<MetadataManager> {
            on { saveToMetadata(any(), any()) }.thenReturn(Completable.complete())
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .saveMetadata(
                ExampleClass("ABC", 123.toBigDecimal()),
                ExampleClass::class.java,
                ExampleClass::class.serializer(),
                100
            )
            .test()
            .assertComplete()

        verify(metadataManager).saveToMetadata("""{"field1":"ABC","field2":"123"}""", 100)
    }

    @Test
    fun `can load json`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","field2":"456"}"""
                )
            )
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(199, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf(ExampleClass("DEF", 456.toBigDecimal()))
    }

    @Test
    fun `can load missing json`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(Maybe.empty())
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(199, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf()
    }

    @Test
    fun `bad json is an error`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","fie..."""
                )
            )
        }
        MetadataRepositoryAdapter(metadataManager, json)
            .loadMetadata(199, ExampleClass::class.serializer(), ExampleClass::class.java)
            .test()
            .assertError(Exception::class.java)
    }

    @Test
    fun `missing fields should not get serialised`() {
        val metadataManager: MetadataManager = mock {
            on { saveToMetadata(any(), any()) }.thenReturn(Completable.complete())
        }

        val metadataRepository = MetadataRepositoryAdapter(metadataManager, json)

        val test = metadataRepository.save(
            BlockchainAccountCredentialsMetadata(
                userId = "UserId",
                lifetimeToken = "lifetimetoken"
            ),
            10
        ).test()

        test.assertComplete()
        Mockito.verify(metadataManager)
            .saveToMetadata(
                "{\"nabu_user_id\":\"UserId\",\"nabu_lifetime_token\":\"lifetimetoken\"}",
                10
            )
    }
}
