package com.blockchain.network.modules

import com.blockchain.enviroment.EnvironmentUrls
import com.blockchain.koin.apiRetrofit
import com.blockchain.koin.bigDecimal
import com.blockchain.koin.bigInteger
import com.blockchain.koin.enableKotlinSerializerFeatureFlag
import com.blockchain.koin.everypayRetrofit
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.kotlinApiRetrofit
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
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class OkHttpInterceptors(val list: List<Interceptor>) : List<Interceptor> by list

private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
    isLenient = true
}

private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

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

    /**
     * This instance converts to Kotlin data classes ONLY; it will break if used to parse data models
     * written with Java + Jackson.
     */
    single(moshiExplorerRetrofit) {
        Retrofit.Builder()
            .baseUrl(getProperty("explorer-api"))
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(kotlinApiRetrofit) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().apiUrl)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(nabu) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().nabuApi)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(status) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().statusUrl)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(apiRetrofit) {
        val kotlinSerializerFeatureFlag: FeatureFlag = get(enableKotlinSerializerFeatureFlag)
        if (kotlinSerializerFeatureFlag.isEnabled) {
            Retrofit.Builder()
                .baseUrl(getProperty("blockchain-api"))
                .client(get())
                .addConverterFactory(jsonConverter)
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
                .addConverterFactory(jsonConverter)
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
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().everypayHostUrl)
            .client(get())
            .addConverterFactory(get<MoshiConverterFactory>())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }
}
