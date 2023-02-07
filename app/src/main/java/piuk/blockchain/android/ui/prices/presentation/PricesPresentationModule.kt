package piuk.blockchain.android.ui.prices.presentation

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.compose.get
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val pricesPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            PricesViewModel(
                walletModeService = get(),
                coincore = get(),
                exchangeRatesDataManager = get(),
                custodialWalletManager = get(),
                pricesPrefs = get()
            )
        }
    }
}
