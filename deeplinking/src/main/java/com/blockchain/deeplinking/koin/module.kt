package com.blockchain.deeplinking.koin

import com.blockchain.deeplinking.navigation.DeeplinkRedirector
import com.blockchain.deeplinking.processor.DeeplinkProcessorV2
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val deeplinkModule = module {
    scope(payloadScopeQualifier) {
        factory {
            DeeplinkProcessorV2()
        }

        scoped {
            DeeplinkRedirector(
                deeplinkProcessorV2 = get()
            )
        }
    }
}
