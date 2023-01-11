package com.blockchain.kycproviders.prove

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.kycproviders.prove.data.ProveRepository
import com.blockchain.kycproviders.prove.domain.ProveService
import com.blockchain.kycproviders.prove.presentation.ProveAuthSDK
import com.blockchain.kycproviders.prove.presentation.ProvePrefillModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val proveModule = module {
    scope(payloadScopeQualifier) {
        factory<ProveService> {
            ProveRepository(
                api = get(),
                mobileAuthSDK = object : ProveAuthSDK {
                    override fun isAuthenticationPossible() {}
                    override fun authenticate(): Boolean = true
                },
            )
        }

        viewModel { params ->
            ProvePrefillModel(
                proveService = get(),
                userService = get(),
                kycTiersStore = get(),
            )
        }
    }
}
