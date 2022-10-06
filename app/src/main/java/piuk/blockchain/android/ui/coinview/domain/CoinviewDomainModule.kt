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
                watchlistDataManager = get(),
                currencyPrefs = get(),
                stakingService = get()
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
            GetAccountActionsUseCase(
                assetActionsComparator = get(),
                dashboardPrefs = get(),
                dispatcher = Dispatchers.IO
            )
        }

        scoped {
            LoadAssetRecurringBuysUseCase(
                tradeDataService = get(),
                custodialWalletManager = get()
            )
        }

        scoped {
            LoadQuickActionsUseCase(
                kycService = get(),
                userFeaturePermissionService = get(),
                custodialWalletManager = get()
            )
        }
    }
}
