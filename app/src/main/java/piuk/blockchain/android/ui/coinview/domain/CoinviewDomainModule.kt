package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.ui.coinview.presentation.CoinviewViewModel

val coinviewDomainModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            GetAssetPriceUseCase
        }
    }
}
