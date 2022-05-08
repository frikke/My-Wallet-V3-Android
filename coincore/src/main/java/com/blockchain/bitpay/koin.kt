package com.blockchain.bitpay

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.serializerExplorerRetrofit
import org.koin.dsl.module

val bitpayModule = module {

    scope(payloadScopeQualifier) {

        factory {
            BitPayDataManager(
                bitPayService = get()
            )
        }

        factory {
            BitPayService(
                environmentConfig = get(),
                retrofit = get(serializerExplorerRetrofit)
            )
        }
    }
}
