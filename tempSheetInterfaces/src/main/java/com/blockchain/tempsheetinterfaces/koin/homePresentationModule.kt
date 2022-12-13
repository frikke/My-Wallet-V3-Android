package com.blockchain.tempsheetinterfaces.koin

import com.blockchain.koin.applicationScope
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.tempsheetinterfaces.fiatactions.FiatActions
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
