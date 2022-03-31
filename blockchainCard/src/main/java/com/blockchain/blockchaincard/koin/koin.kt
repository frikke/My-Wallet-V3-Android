package com.blockchain.blockchaincard.koin

import com.blockchain.blockchaincard.data.BlockchainCardRepositoryImpl
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.BlockchainCardNavigationRouter
import com.blockchain.blockchaincard.viewmodel.BlockchainCardViewModel
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val blockchainCardModule = module {
    scope(payloadScopeQualifier) {
        factory {
            BlockchainCardRepositoryImpl(
                blockchainCardService = get(),
                authenticator = get()
            )
        }.bind(BlockchainCardRepository::class)

        factory {
            BlockchainCardNavigationRouter()
        }

        viewModel {
            BlockchainCardViewModel(blockchainCardRepository = get())
        }
    }
}
