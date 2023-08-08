package com.blockchain.earn.data.koin

import com.blockchain.earn.data.dataresources.active.ActiveRewardsBalanceStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsEligibilityStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsLimitsStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsRatesStore
import com.blockchain.earn.data.dataresources.active.ActiveRewardsWithdrawalsStore
import com.blockchain.earn.data.dataresources.interest.InterestAvailableAssetsStore
import com.blockchain.earn.data.dataresources.interest.InterestBalancesStore
import com.blockchain.earn.data.dataresources.interest.InterestEligibilityStore
import com.blockchain.earn.data.dataresources.interest.InterestLimitsStore
import com.blockchain.earn.data.dataresources.interest.InterestRateForAllStore
import com.blockchain.earn.data.dataresources.interest.InterestRateStore
import com.blockchain.earn.data.dataresources.staking.StakingBalanceStore
import com.blockchain.earn.data.dataresources.staking.StakingBondingStore
import com.blockchain.earn.data.dataresources.staking.StakingEligibilityStore
import com.blockchain.earn.data.dataresources.staking.StakingLimitsStore
import com.blockchain.earn.data.dataresources.staking.StakingRatesStore
import com.blockchain.earn.data.repository.ActiveRewardsRepository
import com.blockchain.earn.data.repository.InterestRepository
import com.blockchain.earn.data.repository.StakingRepository
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.koin.activeRewardsBalanceStore
import com.blockchain.koin.interestBalanceStore
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.stakingBalanceStore
import com.blockchain.storedatasource.FlushableDataSource
import org.koin.dsl.bind
import org.koin.dsl.module

val earnDataModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            ActiveRewardsRatesStore(
                activeRewardsApiService = get()
            )
        }

        scoped(activeRewardsBalanceStore) {
            ActiveRewardsBalanceStore(
                activeRewardsApiService = get()
            )
        }.bind(FlushableDataSource::class)

        scoped {
            ActiveRewardsEligibilityStore(
                activeRewardsApiService = get()
            )
        }

        scoped {
            ActiveRewardsLimitsStore(
                activeRewardsApiService = get(),
                currencyPrefs = get()
            )
        }

        scoped<ActiveRewardsService> {
            ActiveRewardsRepository(
                activeRewardsRateStore = get(),
                activeRewardsEligibilityStore = get(),
                activeRewardsBalanceStore = get(activeRewardsBalanceStore),
                assetCatalogue = get(),
                paymentTransactionHistoryStore = get(),
                activeRewardsLimitsStore = get(),
                currencyPrefs = get(),
                activeRewardsApi = get(),
                historicRateFetcher = get(),
                activeRewardsWithdrawalStore = get()
            )
        }

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

        scoped(stakingBalanceStore) {
            StakingBalanceStore(
                stakingApiService = get()
            )
        }.bind(FlushableDataSource::class)

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
                stakingBalanceStore = get(stakingBalanceStore),
                assetCatalogue = get(),
                paymentTransactionHistoryStore = get(),
                stakingLimitsStore = get(),
                currencyPrefs = get(),
                stakingApi = get(),
                historicRateFetcher = get(),
                stakingBondingStore = get()
            )
        }

        scoped(interestBalanceStore) {
            InterestBalancesStore(
                interestApiService = get()
            )
        }.bind(FlushableDataSource::class)

        scoped {
            InterestAvailableAssetsStore(
                interestApiService = get()
            )
        }

        scoped {
            InterestEligibilityStore(
                interestApiService = get()
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
                interestApiService = get()
            )
        }

        scoped {
            InterestRateForAllStore(
                interestApiService = get()
            )
        }

        scoped<InterestService> {
            InterestRepository(
                assetCatalogue = get(),
                interestBalancesStore = get(interestBalanceStore),
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

        scoped {
            ActiveRewardsWithdrawalsStore(
                activeRewardsApiService = get()
            )
        }

        scoped {
            StakingBondingStore(
                stakingApiService = get()
            )
        }
    }
}
