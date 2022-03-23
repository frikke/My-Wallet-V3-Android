package com.blockchain.network.modules

import com.blockchain.enviroment.EnvironmentUrls
import com.blockchain.koin.apiRetrofit
import com.blockchain.koin.bigDecimal
import com.blockchain.koin.bigInteger
import com.blockchain.koin.disableMoshiSerializerFeatureFlag
import com.blockchain.koin.enableKotlinSerializerFeatureFlag
import com.blockchain.koin.everypayRetrofit
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.kotlinApiRetrofit
import com.blockchain.koin.kotlinJsonConverterFactory
import com.blockchain.koin.moshiExplorerRetrofit
import com.blockchain.koin.moshiInterceptor
import com.blockchain.koin.nabu
import com.blockchain.koin.status
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.serialization.BigDecimalAdapter
import com.blockchain.serialization.BigIntegerAdapter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.squareup.moshi.Moshi
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class OkHttpInterceptors(val list: List<Interceptor>) : List<Interceptor> by list

val apiModule = module {

    moshiInterceptor(bigDecimal) { builder ->
        builder.add(BigDecimalAdapter())
    }

    moshiInterceptor(bigInteger) { builder ->
        builder.add(BigIntegerAdapter())
    }

    single { JacksonConverterFactory.create(ObjectMapper().registerKotlinModule()) }

    single {
        CertificatePinner.Builder()
            .add("api.blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.com", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .build()
    }

    single {
        Moshi.Builder()
            .also {
                get<MoshiBuilderInterceptorList>()
                    .forEach { interceptor -> interceptor.intercept(it) }
            }
            .build()
    }

    single {
        MoshiConverterFactory.create(get())
    }

    single(kotlinJsonConverterFactory) {
        get<Json>().asConverterFactory("application/json".toMediaType())
    }

    /**
     * This instance converts to Kotlin data classes ONLY; it will break if used to parse data models
     * written with Java + Jackson.
     */
    single(moshiExplorerRetrofit) {
        val converterFactory = if (get<FeatureFlag>(disableMoshiSerializerFeatureFlag).isEnabled)
            get<Converter.Factory>(kotlinJsonConverterFactory) else get<MoshiConverterFactory>()

        Retrofit.Builder()
            .baseUrl(getProperty("explorer-api"))
            .client(get())
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(kotlinApiRetrofit) {
        val converterFactory = if (get<FeatureFlag>(disableMoshiSerializerFeatureFlag).isEnabled)
            get<Converter.Factory>(kotlinJsonConverterFactory) else get<MoshiConverterFactory>()

        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().apiUrl)
            .client(get())
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(nabu) {
        val converterFactory = if (get<FeatureFlag>(disableMoshiSerializerFeatureFlag).isEnabled)
            get<Converter.Factory>(kotlinJsonConverterFactory) else get<MoshiConverterFactory>()

        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().nabuApi)
            .client(get())
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(status) {
        val converterFactory = if (get<FeatureFlag>(disableMoshiSerializerFeatureFlag).isEnabled)
            get<Converter.Factory>(kotlinJsonConverterFactory) else get<MoshiConverterFactory>()

        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().statusUrl)
            .client(get())
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(apiRetrofit) {
        val kotlinSerializerFeatureFlag: FeatureFlag = get(enableKotlinSerializerFeatureFlag)
        if (kotlinSerializerFeatureFlag.isEnabled) {
            Retrofit.Builder()
                .baseUrl(getProperty("blockchain-api"))
                .client(get())
                .addConverterFactory(get(kotlinJsonConverterFactory))
                .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
                .build()
        } else {
            Retrofit.Builder()
                .baseUrl(getProperty("blockchain-api"))
                .client(get())
                .addConverterFactory(get<JacksonConverterFactory>())
                .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
                .build()
        }
    }

    single(explorerRetrofit) {
        val kotlinSerializerFeatureFlag: FeatureFlag = get(enableKotlinSerializerFeatureFlag)
        if (kotlinSerializerFeatureFlag.isEnabled) {
            Retrofit.Builder()
                .baseUrl(getProperty("explorer-api"))
                .client(get())
                .addConverterFactory(get(kotlinJsonConverterFactory))
                .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
                .build()
        } else {
            Retrofit.Builder()
                .baseUrl(getProperty("explorer-api"))
                .client(get())
                .addConverterFactory(get<JacksonConverterFactory>())
                .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
                .build()
        }
    }

    single(everypayRetrofit) {
        val converterFactory = if (get<FeatureFlag>(disableMoshiSerializerFeatureFlag).isEnabled)
            get<Converter.Factory>(kotlinJsonConverterFactory) else get<MoshiConverterFactory>()

        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().everypayHostUrl)
            .client(get())
            .addConverterFactory(converterFactory)
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }
}
