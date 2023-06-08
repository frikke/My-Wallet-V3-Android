package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.sell.SellRepository
import com.blockchain.transactions.sell.SellService
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
                walletManager = get(),
                remoteConfigService = get(),
                interestService = get(),
                dismissRecorder = get(),
            )
        }

        scoped<SellService> {
            SellRepository(
                coincore = get(),
                limitsDataManager = get(),
                walletManager = get(),
                simpleBuyService = get(),
                fiatCurrenciesService = get(),
            )
        }
    }
}
