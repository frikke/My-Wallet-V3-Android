package com.blockchain.nfts.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nfts.collection.NftCollectionViewModel
import com.blockchain.nfts.detail.NftDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val nftPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            NftCollectionViewModel(
                coincore = get(),
                nftService = get(),
                oneTimeAccountPersistenceService = get()
            )
        }

        viewModel { (nftId: String, address: String, pageKey: String?) ->
            NftDetailViewModel(
                nftId = nftId,
                address = address,
                pageKey = pageKey,
                nftService = get()
            )
        }
    }
}
