package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.swap.SwapRepository
import com.blockchain.transactions.swap.SwapService
import org.koin.dsl.module

val transactionsDataModule = module {
    scope(payloadScopeQualifier) {
        scoped<SwapService> {
            SwapRepository(
                coincore = get(),
                custodialRepository = get(),
                limitsDataManager = get(),
                walletManager = get()
            )
        }
    }
}
