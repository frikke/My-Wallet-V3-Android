package com.blockchain.earn.koin

import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsSummaryViewModel
import com.blockchain.earn.dashboard.viewmodel.EarnDashboardViewModel
import com.blockchain.earn.interest.viewmodel.InterestSummaryViewModel
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewModel
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.stakingWithdrawalsFeatureFlag
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val earnPresentationModule = module {
    scope(payloadScopeQualifier) {

        viewModel {
            InterestSummaryViewModel(
                coincore = get(),
                interestService = get(),
                exchangeRatesDataManager = get()
            )
        }

        viewModel {
            StakingSummaryViewModel(
                coincore = get(),
                stakingService = get(),
                exchangeRatesDataManager = get(),
                stakingWithdrawalsFF = get(stakingWithdrawalsFeatureFlag)
            )
        }

        viewModel {
            ActiveRewardsSummaryViewModel(
                coincore = get(),
                activeRewardsService = get(),
                exchangeRatesDataManager = get(),
                currencyPrefs = get(),
                activeRewardsWithdrawalsFF = get(activeRewardsWithdrawalsFeatureFlag)
            )
        }

        viewModel {
            EarnDashboardViewModel(
                coincore = get(),
                stakingService = get(),
                exchangeRatesDataManager = get(),
                interestService = get(),
                activeRewardsService = get(),
                userIdentity = get(),
                assetCatalogue = get(),
                custodialWalletManager = get(),
                walletStatusPrefs = get(),
                currencyPrefs = get()
            )
        }
    }
}
