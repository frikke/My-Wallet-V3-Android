package piuk.blockchain.android.ui.coinview.domain

import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val coinviewDomainModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            GetAssetPriceUseCase
        }
    }
}
