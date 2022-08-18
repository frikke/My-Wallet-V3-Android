package com.blockchain.nfts.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nfts.data.repository.NftRepository
import org.koin.dsl.module

val nftDataModule = module {
    scope(payloadScopeQualifier) {
        factory {
            NftRepository(
                nftService = get()
            )
        }
    }
}
