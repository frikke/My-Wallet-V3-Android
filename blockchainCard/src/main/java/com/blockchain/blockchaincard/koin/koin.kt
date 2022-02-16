package com.blockchain.blockchaincard.koin

import com.blockchain.api.nabuApi
import com.blockchain.blockchaincard.data.BcCardService
import com.blockchain.blockchaincard.domain.BcCardApi
import org.koin.dsl.module
import retrofit2.Retrofit

val bcCardsApiModule = module {
    factory {
        val api = get<Retrofit>(nabuApi).create(BcCardApi::class.java)
        BcCardService(
            api
        )
    }
}