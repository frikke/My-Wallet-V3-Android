package com.blockchain.api.coinnetworks

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Create the response
        return Response.Builder()
            .code(200)
            .message(JSON_RESPONSE)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_0)
            .body(ResponseBody.create("application/json".toMediaType(), JSON_RESPONSE.toByteArray()))
            .addHeader("content-type", "application/json")
            .build()
    }

    companion object {
        private const val JSON_RESPONSE = "{\n" +
            "  \"networks\": [\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://www.blockchain.com/eth/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {\n" +
            "        \"chainId\": 1\n" +
            "      },\n" +
            "      \"memos\": false,\n" +
            "      \"name\": \"Ethereum\",\n" +
            "      \"nativeAsset\": \"ETH\",\n" +
            "      \"networkTicker\": \"ETH\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"eth/nodes/rpc\"\n" +
            "      ],\n" +
            "      \"type\": \"EVM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://polygonscan.com/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {\n" +
            "        \"chainId\": 137\n" +
            "      },\n" +
            "      \"memos\": false,\n" +
            "      \"name\": \"Polygon\",\n" +
            "      \"nativeAsset\": \"MATIC.MATIC\",\n" +
            "      \"networkTicker\": \"MATIC\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"matic-bor/nodes/rpc\"\n" +
            "      ],\n" +
            "      \"type\": \"EVM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://bscscan.com/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {\n" +
            "        \"chainId\": 56\n" +
            "      },\n" +
            "      \"memos\": false,\n" +
            "      \"name\": \"Binance Smart Chain\",\n" +
            "      \"nativeAsset\": \"BNB\",\n" +
            "      \"networkTicker\": \"BNB\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"bnb/nodes/rpc\"\n" +
            "      ],\n" +
            "      \"type\": \"EVM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://snowtrace.io/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {\n" +
            "        \"chainId\": 43114\n" +
            "      },\n" +
            "      \"memos\": false,\n" +
            "      \"name\": \"Avalanche\",\n" +
            "      \"nativeAsset\": \"AVAX\",\n" +
            "      \"networkTicker\": \"AVAX\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"avax/nodes/rpc/ext/bc/C/rpc\"\n" +
            "      ],\n" +
            "      \"type\": \"EVM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://www.blockchain.com/btc/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {},\n" +
            "      \"memos\": false,\n" +
            "      \"name\": \"Bitcoin\",\n" +
            "      \"nativeAsset\": \"BTC\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"node1\",\n" +
            "        \"node2\"\n" +
            "      ],\n" +
            "      \"type\": \"BTC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"https://stellarchain.io/transactions\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {},\n" +
            "      \"memos\": true,\n" +
            "      \"name\": \"Stellar\",\n" +
            "      \"nativeAsset\": \"XLM\",\n" +
            "      \"networkTicker\": \"XLM\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"node1\",\n" +
            "        \"node2\"\n" +
            "      ],\n" +
            "      \"type\": \"XLM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"explorerUrl\": \"hhttps://explorer.solana.com/tx\",\n" +
            "      \"feeCurrencies\": [\n" +
            "        \"native\"\n" +
            "      ],\n" +
            "      \"identifiers\": {},\n" +
            "      \"memos\": true,\n" +
            "      \"name\": \"Solana\",\n" +
            "      \"nativeAsset\": \"SOL\",\n" +
            "      \"networkTicker\": \"SOL\",\n" +
            "      \"nodeUrls\": [\n" +
            "        \"node1\",\n" +
            "        \"node2\"\n" +
            "      ],\n" +
            "      \"type\": \"SOL\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"types\": [\n" +
            "    {\n" +
            "      \"derivations\": [\n" +
            "        {\n" +
            "          \"coinType\": 60,\n" +
            "          \"descriptor\": 0,\n" +
            "          \"purpose\": 44\n" +
            "        }\n" +
            "      ],\n" +
            "      \"style\": \"SINGLE\",\n" +
            "      \"type\": \"EVM\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"derivations\": [\n" +
            "        {\n" +
            "          \"coinType\": 501,\n" +
            "          \"descriptor\": 0,\n" +
            "          \"purpose\": 44\n" +
            "        }\n" +
            "      ],\n" +
            "      \"style\": \"SINGLE\",\n" +
            "      \"type\": \"SOL\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"derivations\": [\n" +
            "        {\n" +
            "          \"coinType\": 0,\n" +
            "          \"descriptor\": 0,\n" +
            "          \"purpose\": 44\n" +
            "        },\n" +
            "        {\n" +
            "          \"coinType\": 0,\n" +
            "          \"descriptor\": 1,\n" +
            "          \"purpose\": 84\n" +
            "        }\n" +
            "      ],\n" +
            "      \"style\": \"EXTENDED\",\n" +
            "      \"type\": \"BTC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"derivations\": [\n" +
            "        {\n" +
            "          \"coinType\": 148,\n" +
            "          \"descriptor\": 0,\n" +
            "          \"purpose\": 44\n" +
            "        }\n" +
            "      ],\n" +
            "      \"style\": \"SINGLE\",\n" +
            "      \"type\": \"XLM\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"
    }
}
