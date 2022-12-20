package com.blockchain.payments.vgs

import kotlinx.serialization.json.Json
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module

val tokenizerModule = module {
    val vgsJsonQualifier = StringQualifier("vgsJsonQualifier")

    single(vgsJsonQualifier) {
        Json {
            explicitNulls = false
            encodeDefaults = true
        }
    }

    single {
        VgsCardTokenizerRepository(
            environmentConfig = get(),
            json = get(vgsJsonQualifier)
        )
    }.bind(VgsCardTokenizerService::class)
}
