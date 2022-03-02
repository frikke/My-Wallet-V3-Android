package com.blockchain.deeplinking.koin

import com.blockchain.deeplinking.processor.DeeplinkProcessor
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val deeplinkModule = module {
    scope(payloadScopeQualifier) {
        factory {
            DeeplinkProcessor()
        }
    }
}
