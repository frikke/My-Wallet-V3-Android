package com.blockchain.nfts.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nfts.data.repository.NftRepository
import com.blockchain.nfts.domain.service.NftService
import org.koin.dsl.bind
import org.koin.dsl.module

val nftDataModule = module {
    scope(payloadScopeQualifier) {
        factory {
            NftRepository(
                nftService = get()
            )
        }.bind(NftService::class)
    }
}
