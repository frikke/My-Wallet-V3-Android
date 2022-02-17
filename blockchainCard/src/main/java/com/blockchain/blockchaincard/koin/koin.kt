package com.blockchain.blockchaincard.koin

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.getBaseUrl
import com.blockchain.blockchaincard.data.BcCardDataRepository
import com.blockchain.blockchaincard.data.BcCardService
import com.blockchain.blockchaincard.domain.BcCardApi
import com.blockchain.koin.payloadScopeQualifier
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val bcCardGateway = StringQualifier("bc-card-gateway")

private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
    isLenient = true
}

private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

val bcCardsApiModule = module {

    single(bcCardGateway) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("bc-card-gateway"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(jsonConverter)
            .build()
    }

    scope(payloadScopeQualifier) {
        factory {
            val api = get<Retrofit>(bcCardGateway).create(BcCardApi::class.java)
            BcCardService(
                api
            )
        }

        factory {
            BcCardDataRepository(
                bcCardService = get(),
                authenticator = get()
            )
        }
    }
}