package com.blockchain.addressverification.koin

import com.blockchain.addressverification.BuildConfig
import com.blockchain.addressverification.data.AddressVerificationRepository
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.ui.AddressVerificationInteractor
import com.blockchain.addressverification.ui.AddressVerificationModel
import com.blockchain.addressverification.ui.PlacesClientProvider
import com.blockchain.koin.loqateFeatureFlag
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

        scoped {
            PlacesClientProvider(
                context = get(),
                apiKey = BuildConfig.PLACES_API_KEY,
            )
        }

        viewModel {
            AddressVerificationModel(
                interactor = get()
            )
        }

        factory {
            AddressVerificationInteractor(
                placesClientProvider = get(),
                addressVerificationService = get(),
                loqateFeatureFlag = get(loqateFeatureFlag),
            )
        }
    }
}
