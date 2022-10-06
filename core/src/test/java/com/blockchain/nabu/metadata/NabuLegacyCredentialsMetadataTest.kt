package com.blockchain.nabu.metadata

import com.blockchain.api.blockchainApiModule
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@OptIn(InternalSerializationApi::class)
class NabuLegacyCredentialsMetadataTest : KoinTest {

    private val json: Json by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(blockchainApiModule)
    }

    @Test
    fun `should be valid`() {
        NabuLegacyCredentialsMetadata("userId", "lifeTimeToken").isValid() `should be equal to` true
    }

    @Test
    fun `empty id, should not be valid`() {
        NabuLegacyCredentialsMetadata("", "lifeTimeToken").isValid() `should be equal to` false
    }

    @Test
    fun `empty token, should not be valid`() {
        NabuLegacyCredentialsMetadata("userId", "").isValid() `should be equal to` false
    }

    @Test
    fun `corrupted  properties should get fetched and mapped to normal regardless ordering`() {
        val data = " {\n" +
            "      \"lifetimeToken\": \"23123\",\n" +
            "      \"userId\": \"23131232222\"\n" +
            "    }"

        val metadata = json.decodeFromString(NabuLegacyCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == NabuLegacyCredentialsMetadata(
                "23131232222", "23123", true
            )
        )
    }

    @Test
    fun `normal  properties should get fetched and mapped to normal`() {
        val data = "{\n" +
            "      \"user_id\": \"3213\",\n" +
            "      \"lifetime_token\": \"yyyyy11\"\n" +
            "    }"

        val metadata = json.decodeFromString(NabuLegacyCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == NabuLegacyCredentialsMetadata(
                userId = "3213", lifetimeToken = "yyyyy11"
            )
        )
    }

    @Test
    fun `corrupted  properties should return invalid NabuLegacyCredentialsMetadata`() {
        val data = "{\n" +
            "      \"userqwe_id\": \"322313\",\n" +
            "      \"lifetimwqee_token\": \"yyyy213y11\"\n" +
            "    }"

        val metadata = json.decodeFromString(NabuLegacyCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == NabuLegacyCredentialsMetadata(
                "", ""
            )
        )
    }

    @Test
    fun `normal  properties should get serilised properly`() {
        val metadata = NabuLegacyCredentialsMetadata(
            userId = "3213", lifetimeToken = "yyyyy11"
        )
        val encoded = json.encodeToString(NabuLegacyCredentialsMetadata::class.serializer(), metadata)
        assert(
            encoded == "{\"user_id\":\"3213\",\"lifetime_token\":\"yyyyy11\"}"
        )
    }
}
