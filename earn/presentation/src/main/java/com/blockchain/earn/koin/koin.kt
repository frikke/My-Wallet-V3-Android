package com.blockchain.earn.koin

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val earnPresentationModule = module {
    scope(payloadScopeQualifier) {
    }
}
