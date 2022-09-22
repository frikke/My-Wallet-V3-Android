package com.blockchain.nfts.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nfts.collection.NftCollectionViewModel
import com.blockchain.nfts.detail.NftDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val nftPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            NftCollectionViewModel(nftService = get())
        }

        viewModel {
            NftDetailViewModel(nftService = get())
        }
    }
}
