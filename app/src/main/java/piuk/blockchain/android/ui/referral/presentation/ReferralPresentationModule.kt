package piuk.blockchain.android.ui.referral.presentation

import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val referralPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            ReferralViewModel()
        }
    }
}
