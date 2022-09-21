package com.blockchain.network.modules

import com.blockchain.enviroment.EnvironmentUrls
import com.blockchain.koin.apiRetrofit
import com.blockchain.koin.everypayRetrofit
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.kotlinApiRetrofit
import com.blockchain.koin.kotlinJsonConverterFactory
import com.blockchain.koin.serializerExplorerRetrofit
import com.blockchain.koin.status
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

class OkHttpInterceptors(val list: List<Interceptor>) : List<Interceptor> by list
class OkHttpAuthInterceptor(val interceptor: Interceptor) : Interceptor by interceptor
class OkHttpLoggingInterceptors(val list: List<Interceptor>) : List<Interceptor> by list

val apiModule = module {

    single {
        CertificatePinner.Builder()
            .add("api.blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .add("blockchain.com", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
            .build()
    }

    single(kotlinJsonConverterFactory) {
        get<Json>().asConverterFactory("application/json".toMediaType())
    }

    /**
     * This instance converts to Kotlin data classes ONLY; it will break if used to parse data models
     * written with Java + Jackson.
     */
    single(serializerExplorerRetrofit) {
        Retrofit.Builder()
            .baseUrl(getProperty<String>("explorer-api"))
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(kotlinApiRetrofit) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().apiUrl)
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(status) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().statusUrl)
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(apiRetrofit) {
        Retrofit.Builder()
            .baseUrl(getProperty<String>("blockchain-api"))
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(explorerRetrofit) {
        Retrofit.Builder()
            .baseUrl(getProperty<String>("explorer-api"))
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }

    single(everypayRetrofit) {
        Retrofit.Builder()
            .baseUrl(get<EnvironmentUrls>().everypayHostUrl)
            .client(get())
            .addConverterFactory(get(kotlinJsonConverterFactory))
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .build()
    }
}
