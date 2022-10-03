package com.blockchain.addressverification.koin

import com.blockchain.addressverification.data.AddressVerificationRepository
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.ui.AddressVerificationModel
import com.blockchain.koin.payloadScopeQualifier
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val addressVerificationModule = module {
    scope(payloadScopeQualifier) {
        factory<AddressVerificationService> {
            AddressVerificationRepository(
                authenticator = get(),
                api = get(),
                userService = get(),
            )
        }

        viewModel {
            AddressVerificationModel(
                addressVerificationService = get()
            )
        }
    }
}
