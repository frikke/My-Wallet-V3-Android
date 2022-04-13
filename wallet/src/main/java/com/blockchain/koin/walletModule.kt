package com.blockchain.koin

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.getBaseUrl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import info.blockchain.wallet.api.dust.BchDustService
import info.blockchain.wallet.api.dust.DustApi
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthEndpoints
import info.blockchain.wallet.ethereum.node.EthNodeEndpoints
import info.blockchain.wallet.metadata.MetadataInteractor
import info.blockchain.wallet.metadata.MetadataService
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.multiaddress.MultiAddressFactoryBtc
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.BalanceManagerBtc
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadScopeWiper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val walletModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            PayloadManager(get(), get(), get(), get(), get(), get(), get())
        }

        factory { MultiAddressFactoryBtc(bitcoinApi = get()) }.bind(MultiAddressFactory::class)

        factory { BalanceManagerBtc(bitcoinApi = get()) }

        factory { BalanceManagerBch(bitcoinApi = get()) }
    }

    factory {
        MetadataInteractor(
            metadataService = get()
        )
    }

    single {
        get<Retrofit>(apiRetrofit).create(MetadataService::class.java)
    }

    factory {
        BchDustService(
            api = get<Retrofit>(kotlinApiRetrofit).create(DustApi::class.java),
            apiCode = getProperty("api-code")
        )
    }.bind(DustService::class)

    single {
        object : PayloadScopeWiper {
            override fun wipe() {
                if (!payloadScope.closed) {
                    payloadScope.close()
                }
            }
        }
    }.bind(PayloadScopeWiper::class)

    single(kotlinXApiRetrofit) {
        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    factory {
        EthAccountApi(
            ethEndpoints = get<Retrofit>(apiRetrofit).create(EthEndpoints::class.java),
            ethNodeEndpoints = get<Retrofit>(kotlinXApiRetrofit).create(EthNodeEndpoints::class.java),
            apiCode = getProperty("api-code")
        )
    }
}
