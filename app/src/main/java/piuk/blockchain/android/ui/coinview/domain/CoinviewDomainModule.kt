package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.koin.payloadScopeQualifier
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
                currencyPrefs = get()
            )
        }

        scoped {
            LoadAssetRecurringBuysUseCase(
                tradeDataService = get(),
                custodialWalletManager = get()
            )
        }
    }
}
