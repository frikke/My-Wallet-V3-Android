package com.blockchain.earn.data.koin

import com.blockchain.earn.data.dataresources.interest.InterestAvailableAssetsStore
import com.blockchain.earn.data.dataresources.interest.InterestBalancesStore
import com.blockchain.earn.data.dataresources.interest.InterestEligibilityStore
import com.blockchain.earn.data.dataresources.interest.InterestLimitsStore
import com.blockchain.earn.data.dataresources.interest.InterestRateForAllStore
import com.blockchain.earn.data.dataresources.interest.InterestRateStore
import com.blockchain.earn.data.dataresources.staking.StakingBalanceStore
import com.blockchain.earn.data.dataresources.staking.StakingEligibilityStore
import com.blockchain.earn.data.dataresources.staking.StakingLimitsStore
import com.blockchain.earn.data.dataresources.staking.StakingRatesStore
import com.blockchain.earn.data.repository.InterestRepository
import com.blockchain.earn.data.repository.StakingRepository
import com.blockchain.earn.domain.service.InterestService
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

        scoped {
            InterestBalancesStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestAvailableAssetsStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestEligibilityStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestLimitsStore(
                interestApiService = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            InterestRateStore(
                interestApiService = get(),
            )
        }

        scoped {
            InterestRateForAllStore(
                interestApiService = get(),
            )
        }

        scoped<InterestService> {
            InterestRepository(
                assetCatalogue = get(),
                interestBalancesStore = get(),
                interestEligibilityStore = get(),
                interestAvailableAssetsStore = get(),
                interestLimitsStore = get(),
                interestRateStore = get(),
                paymentTransactionHistoryStore = get(),
                currencyPrefs = get(),
                interestApiService = get(),
                interestAllRatesStore = get(),
                historicRateFetcher = get()
            )
        }
    }
}
