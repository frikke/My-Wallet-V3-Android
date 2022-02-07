package com.blockchain.koin.modules

import com.blockchain.AppVersion
import com.blockchain.koin.apiRetrofit
import com.blockchain.koin.everypayRetrofit
import com.blockchain.koin.explorerRetrofit
import com.blockchain.koin.status
import com.blockchain.nabu.api.status.ApiStatusService
import com.blockchain.nabu.datamanagers.ApiStatus
import com.blockchain.nabu.datamanagers.BlockchainApiStatus
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.Device
import info.blockchain.wallet.api.FeeApi
import info.blockchain.wallet.api.FeeEndpoints
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.WalletExplorerEndpoints
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.settings.SettingsManager
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.everypay.service.EveryPayService
import retrofit2.Retrofit

val serviceModule = module {

    single { SettingsManager(get()) }

    single { get<Retrofit>(explorerRetrofit).create(WalletExplorerEndpoints::class.java) }

    single { get<Retrofit>(apiRetrofit).create(FeeEndpoints::class.java) }

    single { get<Retrofit>(everypayRetrofit).create(EveryPayService::class.java) }

    factory {
        WalletApi(
            explorerInstance = get(),
            api = get()
        )
    }

    factory {
        Payment(
            bitcoinApi = get()
        )
    }

    factory { FeeApi(get()) }

    single { get<Retrofit>(status).create(ApiStatusService::class.java) }

    factory {
        BlockchainApiStatus(get())
    }.bind(ApiStatus::class)

    factory {
        object : ApiCode {
            override val apiCode: String
                get() = getProperty("api-code")
        }
    }.bind(ApiCode::class)

    factory {
        object : Device {
            override val osType: String
                get() = getProperty("os_type")
        }
    }.bind(Device::class)

    factory {
        object : AppVersion {
            override val appVersion: String
                get() = getProperty("app-version")
        }
    }.bind(AppVersion::class)
}
