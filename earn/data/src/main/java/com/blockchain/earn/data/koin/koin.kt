package com.blockchain.earn.data.koin

import com.blockchain.earn.data.dataresources.StakingBalanceStore
import com.blockchain.earn.data.dataresources.StakingEligibilityStore
import com.blockchain.earn.data.dataresources.StakingLimitsStore
import com.blockchain.earn.data.dataresources.StakingRatesStore
import com.blockchain.earn.data.repository.StakingRepository
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.stakingAccountFeatureFlag
import org.koin.dsl.module

val earnDataModule = module {
    scope(payloadScopeQualifier) {

        scoped {
            StakingRatesStore(
                stakingApiService = get()
            )
        }

        scoped {
            StakingEligibilityStore(
                stakingApiService = get()
            )
        }

        scoped {
            StakingBalanceStore(
                stakingApiService = get()
            )
        }

        scoped {
            StakingLimitsStore(
                stakingApiService = get(),
                currencyPrefs = get()
            )
        }

        scoped<StakingService> {
            StakingRepository(
                stakingRatesStore = get(),
                stakingEligibilityStore = get(),
                stakingBalanceStore = get(),
                assetCatalogue = get(),
                stakingFeatureFlag = get(stakingAccountFeatureFlag),
                paymentTransactionHistoryStore = get(),
                stakingLimitsStore = get(),
                currencyPrefs = get(),
                stakingApi = get()
            )
        }
    }
}
