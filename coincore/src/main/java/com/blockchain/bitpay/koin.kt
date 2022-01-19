package com.blockchain.bitpay

import com.blockchain.koin.moshiExplorerRetrofit
import com.blockchain.koin.payloadScopeQualifier
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
                retrofit = get(moshiExplorerRetrofit)
            )
        }
    }
}
