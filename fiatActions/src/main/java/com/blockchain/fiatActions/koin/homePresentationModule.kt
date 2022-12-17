package com.blockchain.fiatActions.koin

import com.blockchain.fiatActions.fiatactions.FiatActionsUseCase
import com.blockchain.koin.applicationScope
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val fiatActionsModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            FiatActionsUseCase(
                scope = get(applicationScope),
                dataRemediationService = get(),
                userIdentity = get(),
                linkedBanksFactory = get(),
                bankService = get()
            )
        }
    }
}
