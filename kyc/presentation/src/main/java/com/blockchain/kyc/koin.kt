package com.blockchain.kyc

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.kyc.email.EmailVerificationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val kycPresentationModule = module {
    scope(payloadScopeQualifier) {
        viewModel {
            EmailVerificationViewModel(
                verificationRequired = false,
                emailUpdater = get(),
                notificationTransmitter = get()
            )
        }
    }
}
