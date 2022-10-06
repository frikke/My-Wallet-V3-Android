package com.blockchain.blockchaincard.koin

import com.blockchain.blockchaincard.data.BlockchainCardRepositoryImpl
import com.blockchain.blockchaincard.domain.BlockchainCardRepository
import com.blockchain.blockchaincard.googlewallet.manager.GoogleWalletManager
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
                eligibilityApiService = get(),
                coincore = get(),
                assetCatalogue = get(),
                userIdentity = get(),
                googleWalletManager = get()
            )
        }.bind(BlockchainCardRepository::class)

        factory {
            GoogleWalletManager(
                context = get()
            )
        }

        viewModel {
            OrderCardViewModel(blockchainCardRepository = get())
        }

        viewModel {
            ManageCardViewModel(blockchainCardRepository = get())
        }
    }
}
