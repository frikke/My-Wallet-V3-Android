package piuk.blockchain.android.ui.coinview.presentation

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val coinviewPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            CoinviewViewModel(
                walletModeService = get(),
                coincore = get(),
                assetCatalogue = get(),
                currencyPrefs = get(),
                labels = get(),
                getAssetPriceUseCase = get(),
                watchlistService = get(),
                loadAssetAccountsUseCase = get(),
                getAccountActionsUseCase = get(),
                loadAssetRecurringBuysUseCase = get(),
                loadQuickActionsUseCase = get(),
                assetService = get(),
                custodialWalletManager = get(),
                recurringBuyService = get(),
                newsService = get(),
                kycService = get()
            )
        }
    }
}
