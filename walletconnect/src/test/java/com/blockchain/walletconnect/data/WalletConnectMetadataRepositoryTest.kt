package com.blockchain.walletconnect.data

import com.blockchain.walletconnect.domain.ClientMeta
import com.blockchain.walletconnect.domain.DAppInfo
import com.blockchain.walletconnect.domain.WalletConnectSession
import com.blockchain.walletconnect.domain.WalletInfo
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class WalletConnectMetadataRepositoryTest {

    private val metadataManager: MetadataManager = mock()
    private val subject = WalletConnectMetadataRepository(metadataManager)

    @Test
    fun `load json sessions from metadata should create the corresponding v1 sessions`() {
        whenever(metadataManager.fetchMetadata(METADATA_WALLET_CONNECT_TYPE)).thenReturn(
            Maybe.just(
                sampleMetadataString
            )
        )

        val test = subject.retrieve().test()

        test.assertValue { sessions ->
            sessions.size == 2 &&
                sessions[0].url == "sessionUrl" &&
                sessions[0].walletInfo == WalletInfo(clientId = "12345-6789-1234", sourcePlatform = "Android") &&
                sessions[0].dAppInfo == DAppInfo(
                chainId = 1,
                peerId = "PEER_ID",
                peerMeta = ClientMeta(
                    description = "description",
                    name = "dappName",
                    url = "dappUrl",
                    icons = listOf("https://random_icon.org")
                )
            ) &&
                sessions[1].url == "sessionUrl2" &&
                sessions[1].walletInfo == WalletInfo(clientId = "12345-6789-123400", sourcePlatform = "Android") &&
                sessions[1].dAppInfo == DAppInfo(
                chainId = 1,
                peerId = "PEER_ID_2",
                peerMeta = ClientMeta(
                    description = "description_2",
                    name = "dappName2",
                    url = "dappUrl2",
                    icons = listOf("https://random_icon_2.org")
                )
            )
        }
    }

    @Test
    fun `load json sessions from metadata with no sessions should not create any v1 sessions`() {
        whenever(metadataManager.fetchMetadata(METADATA_WALLET_CONNECT_TYPE)).thenReturn(
            Maybe.empty()
        )

        val test = subject.retrieve().test()

        test.assertValue { sessions ->
            sessions.isEmpty()
        }
    }

    @Test
    fun `load json sessions from metadata with empty response should not create any v1 sessions`() {
        whenever(metadataManager.fetchMetadata(METADATA_WALLET_CONNECT_TYPE)).thenReturn(Maybe.just("{}"))

        val test = subject.retrieve().test()

        test.assertValue { sessions ->
            sessions.isEmpty()
        }
    }

    @Test
    fun `load json sessions from metadata with null chainID should set chainID to 1`() {
        whenever(metadataManager.fetchMetadata(METADATA_WALLET_CONNECT_TYPE)).thenReturn(
            Maybe.just(
                "{\n" +
                    "    \"sessions\":\n" +
                    "    {\n" +
                    "        \"v1\":\n" +
                    "        [\n" +
                    "            {\n" +
                    "                \"dAppInfo\":\n" +
                    "                {\n" +
                    "                    \"peerId\": \"PEER_ID\",\n" +
                    "                    \"peerMeta\":\n" +
                    "                    {\n" +
                    "                        \"description\": \"description\",\n" +
                    "                        \"icons\":\n" +
                    "                        [\n" +
                    "                            \"https://random_icon.org\"\n" +
                    "                        ],\n" +
                    "                        \"name\": \"dappName\",\n" +
                    "                        \"url\": \"dappUrl\"\n" +
                    "                    }\n" +
                    "                },\n" +
                    "                \"url\": \"sessionUrl\",\n" +
                    "                \"walletInfo\":\n" +
                    "                {\n" +
                    "                    \"clientId\": \"12345-6789-1234\",\n" +
                    "                    \"sourcePlatform\": \"Android\"\n" +
                    "                }\n" +
                    "            }\n" +
                    "           \n" +
                    "        ]\n" +
                    "    }\n" +
                    "}"
            )
        )

        val test = subject.retrieve().test()

        test.assertValue { sessions ->
            sessions.size == 1 &&
                sessions[0].url == "sessionUrl" &&
                sessions[0].walletInfo == WalletInfo(clientId = "12345-6789-1234", sourcePlatform = "Android") &&
                sessions[0].dAppInfo == DAppInfo(
                chainId = 1,
                peerId = "PEER_ID",
                peerMeta = ClientMeta(
                    description = "description",
                    name = "dappName",
                    url = "dappUrl",
                    icons = listOf("https://random_icon.org")
                )
            )
        }
    }

    @Test
    fun `store a new session should append the new one to the existing metadata payload`() {
        whenever(metadataManager.fetchMetadata(13)).thenReturn(
            Maybe.just(
                sampleMetadataString
            )
        )

        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(
            Completable.complete()
        )

        val test = subject.store(
            WalletConnectSession(
                url = "NewUrl",
                walletInfo = WalletInfo(clientId = "clientId", sourcePlatform = "Android"),
                dAppInfo = DAppInfo(
                    peerId = "peerIdNew",
                    peerMeta = ClientMeta(
                        description = "random description",
                        name = "superb dappName",
                        url = "superb dappUrl",
                        icons = listOf("https://random_awesome_icon.org")
                    ),
                    chainId = 12
                )
            )
        ).test()

        test.assertComplete()
        verify(metadataManager).saveToMetadata(
            "{\"sessions\":{\"v1\":[{\"url\":\"sessionUrl\",\"dAppInfo\":{\"chainId\":1,\"peerId\":\"P" +
                "EER_ID\",\"peerMeta\":{\"name\":\"dappName\",\"url\":\"dappUrl\",\"icons\":[\"https://random_icon" +
                ".org\"],\"description\":\"description\"}},\"walletInfo\":{\"sourcePlatform\":\"Android\",\"clien" +
                "tId\":\"12345-6789-1234\"}},{\"url\":\"sessionUrl2\",\"dAppInfo\":{\"chainId\":1,\"peerId\":\"PEER_" +
                "ID_2\",\"peerMeta\":{\"name\":\"dappName2\",\"url\":\"dappUrl2\",\"icons\":[\"https://random_i" +
                "con_2.org\"],\"description\":\"description_2\"}},\"walletIn" +
                "fo\":{\"sourcePlatform\":\"Android\",\"clien" +
                "tId\":\"12345-6789-123400\"}},{\"url\":\"NewUrl\",\"dAppInfo\":{\"chainId\":12,\"peerId\":\"peerIdN" +
                "ew\",\"peerMeta\":{\"name\":\"superb dappName\",\"url\":\"superb dappUrl\",\"icons\":[\"https://r" +
                "andom_awesome_icon.org\"],\"description\":\"random description\"}},\"walletI" +
                "nfo\":{\"sourcePlatform\":\"Android\",\"clientId\":\"clientId\"}}]}}",
            METADATA_WALLET_CONNECT_TYPE
        )
    }

    private val METADATA_WALLET_CONNECT_TYPE = 13

    private val sampleMetadataString = "{\n" +
        "    \"sessions\":\n" +
        "    {\n" +
        "        \"v1\":\n" +
        "        [\n" +
        "            {\n" +
        "                \"dAppInfo\":\n" +
        "                {\n" +
        "                    \"chainId\": 1,\n" +
        "                    \"peerId\": \"PEER_ID\",\n" +
        "                    \"peerMeta\":\n" +
        "                    {\n" +
        "                        \"description\": \"description\",\n" +
        "                        \"icons\":\n" +
        "                        [\n" +
        "                            \"https://random_icon.org\"\n" +
        "                        ],\n" +
        "                        \"name\": \"dappName\",\n" +
        "                        \"url\": \"dappUrl\"\n" +
        "                    }\n" +
        "                },\n" +
        "                \"url\": \"sessionUrl\",\n" +
        "                \"walletInfo\":\n" +
        "                {\n" +
        "                    \"clientId\": \"12345-6789-1234\",\n" +
        "                    \"sourcePlatform\": \"Android\"\n" +
        "                }\n" +
        "            },\n" +
        "            {\n" +
        "                \"dAppInfo\":\n" +
        "                {\n" +
        "                    \"chainId\": 1,\n" +
        "                    \"peerId\": \"PEER_ID_2\",\n" +
        "                    \"peerMeta\":\n" +
        "                    {\n" +
        "                        \"description\": \"description_2\",\n" +
        "                        \"icons\":\n" +
        "                        [\n" +
        "                            \"https://random_icon_2.org\"\n" +
        "                        ],\n" +
        "                        \"name\": \"dappName2\",\n" +
        "                        \"url\": \"dappUrl2\"\n" +
        "                    }\n" +
        "                },\n" +
        "                \"url\": \"sessionUrl2\",\n" +
        "                \"walletInfo\":\n" +
        "                {\n" +
        "                    \"clientId\": \"12345-6789-123400\",\n" +
        "                    \"sourcePlatform\": \"Android\"\n" +
        "                }\n" +
        "            }\n" +
        "        ]\n" +
        "    }\n" +
        "}"
}
