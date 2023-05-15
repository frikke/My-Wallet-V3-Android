package com.blockchain.nabu.metadata

import com.blockchain.api.blockchainApiModule
import com.blockchain.testutils.KoinTestRule
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.inject

@OptIn(InternalSerializationApi::class)
class BlockchainAccountCredentialsMetadataTest : KoinTest {

    val json: Json by inject()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(blockchainApiModule)
    }

    @Test
    fun `corrupted  properties should get fetched and mapped to normal regardless the order`() {
        val data = "{\n" +
            "   \n" +
            "       \"exchangeUserId\":\"wqeqeqw\",\n" +
            "      \"lifetimeToken\": \"sadasda\",\n" +
            "     \n" +
            "      \"exchangeLifetimeToken\":\"12312\",\n" +
            "         \"userId\": \"3213\"\n" +
            "    }"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == BlockchainAccountCredentialsMetadata(
                userId = "3213",
                lifetimeToken = "sadasda",
                exchangeUserId = "wqeqeqw",
                exchangeLifetimeToken = "12312",
                isCorrupted = true

            )
        )
    }

    @Test
    fun `corrupted with only userId and token should be mapped to normal one`() {
        val data = "{\n" +
            "            \"userId\": \"23123\",\n" +
            "            \"lifetimeToken\": \"23131lifetimeToken232222\"\n" +
            "        }"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == BlockchainAccountCredentialsMetadata(
                userId = "23123",
                lifetimeToken = "23131lifetimeToken232222",
                exchangeUserId = null,
                exchangeLifetimeToken = null,
                isCorrupted = true

            )
        )
    }

    @Test
    fun `corrupted with only userId and token should be mapped to normal one regardless the order`() {
        val data = " {\n" +
            "        \"lifetimeToken\": \"23131lifetimeToken232222\",\n" +
            "        \"userId\": \"23123\"\n" +
            "\n" +
            "    }"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == BlockchainAccountCredentialsMetadata(
                userId = "23123",
                lifetimeToken = "23131lifetimeToken232222",
                exchangeUserId = null,
                exchangeLifetimeToken = null,
                isCorrupted = true

            )
        )
    }

    @Test
    fun `NOT corrupted with only userId and token should be mapped to normal one`() {
        val data = "{\n" +
            "            \"nabu_user_id\": \"23123\",\n" +
            "            \"nabu_lifetime_token\": \"23131lifetimeToken232222\"\n" +
            "        }"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == BlockchainAccountCredentialsMetadata(
                userId = "23123",
                lifetimeToken = "23131lifetimeToken232222",
                exchangeUserId = null,
                exchangeLifetimeToken = null,
                isCorrupted = false
            )
        )
    }

    @Test
    fun `NOT corrupted should remain not corrupted after desirialisation`() {
        val data = "{\n" +
            "       \"nabu_user_id\": \"23131lifetimeToken232222\",\n" +
            "      \"nabu_lifetime_token\": \"23123\",\n" +
            "     \"exchange_lifetime_token\": \"exchange_lfsadifetime_token\",\n" +
            "       \"exchange_user_id\": \"exchange_user_idasfsad\"\n" +
            "     \n" +
            "    }"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)
        assert(
            metadata == BlockchainAccountCredentialsMetadata(
                userId = "23131lifetimeToken232222",
                lifetimeToken = "23123",
                exchangeUserId = "exchange_user_idasfsad",
                exchangeLifetimeToken = "exchange_lfsadifetime_token",
                isCorrupted = false
            )
        )
    }

    @Test
    fun `Encoding should work as expected`() {
        val data = BlockchainAccountCredentialsMetadata(
            userId = "userId",
            lifetimeToken = "lifetimeToken",
            exchangeUserId = "exchangeUserId",
            exchangeLifetimeToken = "exchangeLifetimeToken",
            isCorrupted = false
        )
        val encoded = json.encodeToString(data)
        assert(
            encoded == "{\"nabu_user_id\":\"userId\",\"nabu_lifetime_token\":\"lifetimeToken\",\"exchange_user_id\":" +
                "\"exchangeUserId\",\"exchange_lifetime_token\":\"exchangeLifetimeToken\"}"
        )
    }

    @Test
    fun `Encoding should encode only the values present`() {
        val data = BlockchainAccountCredentialsMetadata(
            userId = "userIewqed",
            lifetimeToken = "lifetimeTokenasdas",
            isCorrupted = false
        )
        val encoded = json.encodeToString(data)
        assert(
            encoded == "{\"nabu_user_id\":\"userIewqed\",\"nabu_lifetime_token\":\"lifetimeTokenasdas\"}"
        )
    }

    @Test
    fun `Decode with null values should be decoded successfully`() {
        val data =
            " {\"exchange_user_id\":null,\"exchange_lifetime_token\":null,\"nabu_lifetim" +
                "e_token\":\"23423-fef1-4344-a0d3-60657110e6b0\",\"nabu_user_id\":\"43243-2" +
                "7d3-4a31-b244-423423\",\"CORRUPTED_KEY\":false}"

        val metadata = json.decodeFromString(BlockchainAccountCredentialsMetadata::class.serializer(), data)

        assert(
            metadata.exchangeUserId == null && metadata.exchangeLifetimeToken == null
        )
    }
}
