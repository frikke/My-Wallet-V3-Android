package com.blockchain.fiatActions.koin

import com.blockchain.fiatActions.fiatactions.FiatActions
import com.blockchain.koin.applicationScope
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val fiatActionsModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            FiatActions(
                scope = get(applicationScope),
                dataRemediationService = get(),
                userIdentity = get(),
                linkedBanksFactory = get(),
                bankService = get()
            )
        }
    }
}
