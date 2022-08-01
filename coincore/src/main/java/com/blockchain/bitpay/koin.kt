package com.blockchain.bitpay

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.serializerExplorerRetrofit
import org.koin.dsl.module
import retrofit2.Retrofit

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
                service = get<Retrofit>(serializerExplorerRetrofit).create(BitPay::class.java)
            )
        }
    }
}
