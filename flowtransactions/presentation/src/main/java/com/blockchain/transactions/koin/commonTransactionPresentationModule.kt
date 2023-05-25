package com.blockchain.transactions.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.transactions.common.OnChainDepositEngineInteractor
import org.koin.dsl.module

val commonTransactionsPresentationModule = module {
    scope(payloadScopeQualifier) {
        factory {
            OnChainDepositEngineInteractor(
                custodialWalletManager = get(),
                exchangeRatesDataManager = get(),
            )
        }
    }
}
