package com.blockchain.koin.modules

import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.rating.data.CHECKMARKET_URL

val appProperties = mapOf(
    "app-version" to BuildConfig.VERSION_NAME,
    "os_type" to "android"
)

val keys = mapOf(
    "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3",
    "site-key" to BuildConfig.RECAPTCHA_SITE_KEY
)

val urls = mapOf(
    "HorizonURL" to BuildConfig.HORIZON_URL,
    "explorer-api" to BuildConfig.EXPLORER_URL,
    "blockchain-api" to BuildConfig.API_URL,
    "wallet-pubkey-api" to "${BuildConfig.API_URL}wallet-pubkey/",
    "evm-nodes-api" to BuildConfig.EVM_NODE_API_URL,
    "unified-activity-ws" to "${BuildConfig.UNIFIED_ACTIVITY_WS_URL}",
    "nabu-api" to "${BuildConfig.API_URL}nabu-gateway/",
    "wallet-helper-url" to BuildConfig.WALLET_HELPER_URL,
    CHECKMARKET_URL to BuildConfig.CHECKMARKET_URL,
)
