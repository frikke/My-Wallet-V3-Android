package piuk.blockchain.android.ui.referral.presentation

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val referralPresentationModule = module {
    viewModel {
        ReferralViewModel()
    }
}
