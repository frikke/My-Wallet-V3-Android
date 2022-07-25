package piuk.blockchain.android.ui.educational.walletmodes

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val educationalWalletModesPresentationModule = module {
    viewModel {
        EducationalWalletModeViewModel(educationalScreensPrefs = get())
    }
}
