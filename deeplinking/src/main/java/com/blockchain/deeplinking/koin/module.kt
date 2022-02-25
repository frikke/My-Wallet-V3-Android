package com.blockchain.deeplinking.koin

import com.blockchain.deeplinking.processor.DeeplinkProcessor
import org.koin.dsl.module
import com.blockchain.koin.payloadScopeQualifier


val deeplinkModule = module {
    scope(payloadScopeQualifier) {
        factory {
            DeeplinkProcessor()
        }
    }
}
