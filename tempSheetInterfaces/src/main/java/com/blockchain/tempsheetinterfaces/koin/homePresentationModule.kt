package com.blockchain.tempsheetinterfaces.koin

import com.blockchain.koin.applicationScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActionsUseCase
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
