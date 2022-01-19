package com.blockchain.enviroment

interface EnvironmentUrls {
    val apiUrl: String
    val everypayHostUrl: String
    val statusUrl: String
    val nabuApi: String
        get() = "${apiUrl}nabu-gateway/"
}
