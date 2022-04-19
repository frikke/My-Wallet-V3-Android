package info.blockchain.wallet.ethereum

import info.blockchain.wallet.ethereum.data.EthAddressResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

class EthAddressResponseTest {

    private val data = "{\n" +
        "  \"0x6E05e9044a36D9B9Fe9044a36D9B9F82FC82FCB1f9FE708f7a34C8581\": {\n" +
        "    \"id\": 0,\n" +
        "    \"txn_count\": 27,\n" +
        "    \"account\": \"0x6e05e904e9044a36D9B9F82FC9fe708f7a34c8581\",\n" +
        "    \"accountType\": 0,\n" +
        "    \"balance\": \"217968902283300003\",\n" +
        "    \"nonce\": 17,\n" +
        "    \"firstTime\": null,\n" +
        "    \"numNormalTxns\": 27,\n" +
        "    \"numInternalTxns\": 0,\n" +
        "    \"totalReceived\": \"4610647420433313494\",\n" +
        "    \"totalSent\": \"4364373008150013491\",\n" +
        "    \"totalFee\": \"28305510000000000\",\n" +
        "    \"createdBy\": null,\n" +
        "    \"createdIn\": null,\n" +
        "    \"txns\": [\n" +
        "      {\n" +
        "        \"blockNumber\": 14140846,\n" +
        "        \"timeStamp\": 1643995499,\n" +
        "        \"hash\": \"0x6ce9044a36D9B9F82FC39481a8e9044a36D9B9F82FCbb39298ccfce0b20ff3bfb4cf\",\n" +
        "        \"failFlag\": false,\n" +
        "        \"errorDescription\": null,\n" +
        "        \"nonce\": \"0x15\",\n" +
        "        \"blockHash\": \"0x16f72c5895ce1346d1c8e7ee9044a36D9B9F82FC3de3d95d4a87ebec3a5ff6\",\n" +
        "        \"transactionIndex\": 39,\n" +
        "        \"from\": \"0xb614ac2df0485ae90762d7227373b1ba737e0f8c\",\n" +
        "        \"to\": \"0x6e05e9044a36d9b9f82fcb1f9fe708f7a34c8581\",\n" +
        "        \"value\": \"1702347878193605\",\n" +
        "        \"gas\": 21000,\n" +
        "        \"gasPrice\": 159000000000,\n" +
        "        \"gasUsed\": 21000,\n" +
        "        \"input\": \"0x\",\n" +
        "        \"internalFlag\": false,\n" +
        "        \"contractAddress\": null\n" +
        "      },\n" +
        "      {\n" +
        "        \"blockNumber\": 13153360,\n" +
        "        \"timeStamp\": 1630680298,\n" +
        "        \"hash\": \"0x97e0ee9044a36D9B9F82FCc53699d3846e9044a36D9B9F82FC5a6ed6f1b3a5\",\n" +
        "        \"failFlag\": false,\n" +
        "        \"errorDescription\": null,\n" +
        "        \"nonce\": \"0x10\",\n" +
        "        \"blockHash\": \"0x704f13c623d73e9044a36D9B9F82FCe6896a861fe58f2aebbde97cc72\",\n" +
        "        \"transactionIndex\": 200,\n" +
        "        \"from\": \"0x6e05e9044ae9044a36D9B9F82FC34c8581\",\n" +
        "        \"to\": \"0x6e05e90e9044a36D9B9F82FC1f9fe708f7a34c8581\",\n" +
        "        \"value\": \"1043830440183297\",\n" +
        "        \"gas\": 21000,\n" +
        "        \"gasPrice\": 148000000000,\n" +
        "        \"gasUsed\": 21000,\n" +
        "        \"input\": \"0x\",\n" +
        "        \"internalFlag\": false,\n" +
        "        \"contractAddress\": null\n" +
        "      }\n" +
        "    ],\n" +
        "    \"txnOffset\": 0\n" +
        "  }\n" +
        "}"

    @Test
    fun `EthAddressResponse should be parsed normally when using kotlix searialiser`() {
        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
        }

        val result = json.decodeFromString<HashMap<String, EthAddressResponse>>(data)
        assert(result.size == 1)
        assert(
            result["0x6E05e9044a36D9B9Fe9044a36D9B9F82FC82FCB1f9FE708f7a34C8581"]!!.getId() == 0 &&
                result["0x6E05e9044a36D9B9Fe9044a36D9B9F82FC82FCB1f9FE708f7a34C8581"]!!.getAccount()
                == "0x6e05e904e9044a36D9B9F82FC9fe708f7a34c8581" &&
                result["0x6E05e9044a36D9B9Fe9044a36D9B9F82FC82FCB1f9FE708f7a34C8581"]!!.getAccountType()
                == 0 &&
                result["0x6E05e9044a36D9B9Fe9044a36D9B9F82FC82FCB1f9FE708f7a34C8581"]!!.getBalance() ==
                "217968902283300003".toBigInteger()
        )
    }
}
