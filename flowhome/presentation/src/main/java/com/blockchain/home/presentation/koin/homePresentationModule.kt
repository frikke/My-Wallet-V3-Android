package com.blockchain.home.presentation.koin

import com.blockchain.home.presentation.accouncement.AnnouncementsViewModel
import com.blockchain.home.presentation.activity.detail.custodial.CustodialActivityDetailViewModel
import com.blockchain.home.presentation.activity.detail.privatekey.PrivateKeyActivityDetailViewModel
import com.blockchain.home.presentation.activity.list.custodial.CustodialActivityViewModel
import com.blockchain.home.presentation.activity.list.privatekey.PrivateKeyActivityViewModel
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.EmptyScreenViewModel
import com.blockchain.home.presentation.dapps.HomeDappsViewModel
import com.blockchain.home.presentation.failedbalances.FailedBalancesViewModel
import com.blockchain.home.presentation.fiat.actions.FiatActionsNavigator
import com.blockchain.home.presentation.fiat.fundsdetail.FiatFundsDetailViewModel
import com.blockchain.home.presentation.handhold.HandholdViewModel
import com.blockchain.home.presentation.news.NewsViewModel
import com.blockchain.home.presentation.onboarding.custodial.CustodialIntroViewModel
import com.blockchain.home.presentation.onboarding.defi.DefiIntroViewModel
import com.blockchain.home.presentation.quickactions.QuickActionsViewModel
import com.blockchain.home.presentation.recurringbuy.detail.RecurringBuysDetailViewModel
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysViewModel
import com.blockchain.home.presentation.referral.ReferralViewModel
import com.blockchain.koin.dexFeatureFlag
import com.blockchain.koin.iterableAnnouncementsFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.walletConnectV1FeatureFlag
import com.blockchain.koin.walletConnectV2FeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val homePresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            CustodialIntroViewModel(
                walletStatusPrefs = get(),
                userFeaturePermissionService = get()
            )
        }

        viewModel {
            DefiIntroViewModel(walletStatusPrefs = get())
        }

        viewModel {
            AnnouncementsViewModel(
                walletModeService = get(),
                backupPhraseService = get(),
                announcementsService = get(),
                iterableAnnouncementsFF = get(iterableAnnouncementsFeatureFlag)
            )
        }

        viewModel {
            FailedBalancesViewModel(
                unifiedBalancesService = get()
            )
        }

        viewModel {
            AssetsViewModel(
                homeAccountsService = get(),
                currencyPrefs = get(),
                exchangeRates = get(),
                filterService = get(),
                assetCatalogue = get(),
                walletModeService = get(),
                coincore = get(),
                dispatcher = Dispatchers.IO
            )
        }

        viewModel { (fiatTicker: String) ->
            FiatFundsDetailViewModel(
                fiatTicker = fiatTicker,
                homeAccountsService = get(),
                fiatActions = get()
            )
        }

        scoped { (scope: CoroutineScope) ->
            FiatActionsNavigator(
                scope = scope,
                fiatActions = get()
            )
        }

        viewModel { (
            homeVm: AssetsViewModel,
            pkwActivityViewModel: PrivateKeyActivityViewModel,
            custodialActivityViewModel: CustodialActivityViewModel
        ) ->
            EmptyScreenViewModel(
                homeAssetsViewModel = homeVm,
                walletModeService = get(),
                pkwActivityViewModel = pkwActivityViewModel,
                custodialActivityViewModel = custodialActivityViewModel
            )
        }

        viewModel {
            PrivateKeyActivityViewModel(
                unifiedActivityService = get(),
                walletModeService = get()
            )
        }

        viewModel {
            RecurringBuysViewModel(
                recurringBuyService = get(),
                dispatcher = Dispatchers.IO
            )
        }

        viewModel { (recurringBuyId: String) ->
            RecurringBuysDetailViewModel(
                recurringBuyId = recurringBuyId,
                recurringBuyService = get(),
                bankService = get(),
                cardService = get()
            )
        }

        viewModel {
            CustodialActivityViewModel(
                custodialActivityService = get(),
                walletModeService = get()
            )
        }

        viewModel { (txId: String) ->
            PrivateKeyActivityDetailViewModel(
                activityTxId = txId,
                unifiedActivityService = get()
            )
        }

        viewModel { (txId: String) ->
            CustodialActivityDetailViewModel(
                activityTxId = txId,
                custodialActivityService = get(),
                paymentMethodService = get(),
                cardService = get(),
                bankService = get(),
                coincore = get(),
                defaultLabels = get(),
                recurringBuyService = get()
            )
        }

        viewModel {
            QuickActionsViewModel(
                coincore = get(),
                dexFeatureFlag = get(dexFeatureFlag),
                quickActionsService = get(),
                fiatCurrenciesService = get(),
                fiatActions = get(),
                userFeaturePermissionService = get(),
                dispatcher = Dispatchers.IO,
                handholdService = get(),
                walletModeService = get(),
                kycService = get()
            )
        }

        viewModel {
            ReferralViewModel(
                referralService = get()
            )
        }

        viewModel {
            HomeDappsViewModel(
                sessionsRepository = get(),
                walletConnectService = get(),
                walletConnectV2Service = get(),
                walletConnectV2FeatureFlag = get(walletConnectV2FeatureFlag),
                walletConnectV1FeatureFlag = get(walletConnectV1FeatureFlag)
            )
        }

        viewModel {
            NewsViewModel(
                walletModeService = get(),
                newsService = get(),
                dispatcher = Dispatchers.IO
            )
        }

        viewModel {
            HandholdViewModel(
                handholdService = get(),
                kycService = get(),
                walletModeService = get(),
                dispatcher = Dispatchers.IO
            )
        }
    }
}
