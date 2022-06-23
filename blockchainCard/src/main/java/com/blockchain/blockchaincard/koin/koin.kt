package com.blockchain.blockchaincard.koin

import com.blockchain.blockchaincard.data.BlockchainCardRepositoryImpl
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.viewmodel.managecard.ManageCardViewModel
import com.blockchain.blockchaincard.viewmodel.ordercard.OrderCardViewModel
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val blockchainCardModule = module {
    scope(payloadScopeQualifier) {
        factory {
            BlockchainCardRepositoryImpl(
                blockchainCardService = get(),
                authenticator = get(),
                coincore = get(),
                assetCatalogue = get(),
                userIdentity = get()
            )
        }.bind(BlockchainCardRepository::class)

        viewModel {
            OrderCardViewModel(blockchainCardRepository = get())
        }

        viewModel {
            ManageCardViewModel(blockchainCardRepository = get())
        }
    }
}
