package com.blockchain.unifiedcryptowallet.data.koin

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.store.Store
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesRepository
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesStore
import com.blockchain.unifiedcryptowallet.data.balances.UnifiedBalancesSubscribeStore
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import org.koin.dsl.bind
import org.koin.dsl.module

val unifiedCryptoWalletModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            UnifiedBalancesRepository(
                networkAccountsService = get(),
                unifiedBalancesStore = get(),
                unifiedBalancesSubscribeStore = get(),
                assetCatalogue = get(),
                currencyPrefs = get()
            )
        }.bind(UnifiedBalancesService::class)

        scoped {
            UnifiedBalancesSubscribeStore(
                selfCustodyService = get(),
                unifiedBalancesStore = get()
            )
        }

        scoped {
            UnifiedBalancesStore(
                selfCustodyService = get(),
                currencyPrefs = get()
            )
        }.bind(Store::class)
    }
}
