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
                currencyPrefs = get(),
                labels = get(),
                getAssetPriceUseCase = get(),
                loadAssetAccountsUseCase = get(),
                getAccountActionsUseCase = get(),
                loadAssetRecurringBuysUseCase = get(),
                loadQuickActionsUseCase = get(),
                assetService = get()
            )
        }
    }
}
