package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.koin.payloadScopeQualifier
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val coinviewDomainModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            GetAssetPriceUseCase
        }

        scoped {
            LoadAssetAccountsUseCase(
                walletModeService = get(),
                interestService = get(),
                exchangeRatesDataManager = get(),
                currencyPrefs = get(),
                stakingService = get(),
                activeRewardsService = get()
            )
        }

        scoped {
            GetAccountActionsUseCase(
                assetActionsComparator = get(),
                dashboardPrefs = get(),
                dispatcher = Dispatchers.IO
            )
        }

        scoped {
            LoadAssetRecurringBuysUseCase(
                recurringBuyService = get(),
                custodialWalletManager = get()
            )
        }

        scoped {
            LoadQuickActionsUseCase(
                userFeaturePermissionService = get(),
                custodialWalletManager = get(),
                kycService = get(),
                dexNetworkService = get()
            )
        }
    }
}
