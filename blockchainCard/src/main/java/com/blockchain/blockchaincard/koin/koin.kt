package com.blockchain.blockchaincard.koin

import com.blockchain.api.adapters.OutcomeCallAdapterFactory
import com.blockchain.api.getBaseUrl
import com.blockchain.blockchaincard.data.BlockchainCardRepositoryImpl
import com.blockchain.blockchaincard.domain.BlockchainCardService
import com.blockchain.api.blockchainCard.api.BlockchainCardApi
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.koin.payloadScopeQualifier
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val bcCardGateway = StringQualifier("card-issuing")

private val json = Json {
    explicitNulls = false
    ignoreUnknownKeys = true
    isLenient = true
}

private val jsonConverter = json.asConverterFactory("application/json".toMediaType())

val bcCardsApiModule = module {

    single(bcCardGateway) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("card-issuing"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addCallAdapterFactory(get<OutcomeCallAdapterFactory>())
            .addConverterFactory(jsonConverter)
            .build()
    }

    scope(payloadScopeQualifier) {
        factory {
            val api = get<Retrofit>(bcCardGateway).create(BlockchainCardApi::class.java)
            BlockchainCardService(
                api
            )
        }

        factory {
            BlockchainCardRepositoryImpl(
                blockchainCardService = get(),
                authenticator = get()
            )
        }

        factory {
            BlockchainCardNavigationRouter()
        }

        viewModel {
            BlockchainCardViewModel(blockchainCardRepository = get())
        }
    }
}
