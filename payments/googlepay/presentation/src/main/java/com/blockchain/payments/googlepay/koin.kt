package com.blockchain.payments.googlepay

import com.blockchain.enviroment.Environment
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptor
import com.blockchain.payments.googlepay.interceptor.GooglePayResponseInterceptorImpl
import com.blockchain.payments.googlepay.interceptor.PaymentDataMapper
import com.blockchain.payments.googlepay.manager.GooglePayManager
import com.blockchain.payments.googlepay.manager.GooglePayManagerImpl
import com.blockchain.payments.googlepay.manager.GooglePayViewUtils
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module

val googlePayPresentationModule = module {

    val googlePayPresentationJsonQualifier = StringQualifier("googlePayPresentationJsonQualifier")

    single(googlePayPresentationJsonQualifier) {
        Json {
            explicitNulls = false
            encodeDefaults = true
        }
    }

    single {
        val environmentConfig: EnvironmentConfig = get()

        /**
         * Changing this to ENVIRONMENT_PRODUCTION will make the API return chargeable card information.
         * Please refer to the documentation to read about the required steps needed to enable
         * ENVIRONMENT_PRODUCTION.
         *
         * @value #PAYMENTS_ENVIRONMENT
         */
        val paymentsEnvironment: Int = if (environmentConfig.environment == Environment.PRODUCTION) {
            WalletConstants.ENVIRONMENT_PRODUCTION
        } else {
            WalletConstants.ENVIRONMENT_TEST
        }

        Wallet.getPaymentsClient(
            androidContext(),
            Wallet.WalletOptions.Builder()
                .setEnvironment(paymentsEnvironment)
                .build()
        )
    }

    single<GooglePayManager> {
        GooglePayManagerImpl(
            paymentsClient = get(),
            json = get(googlePayPresentationJsonQualifier)
        )
    }

    single {
        GooglePayViewUtils(
            paymentsClient = get(),
            json = get(googlePayPresentationJsonQualifier)
        )
    }

    single {
        PaymentDataMapper()
    }

    factory<GooglePayResponseInterceptor> {
        GooglePayResponseInterceptorImpl(
            paymentDataMapper = get(),
            coroutineContext = Dispatchers.IO
        )
    }
}
