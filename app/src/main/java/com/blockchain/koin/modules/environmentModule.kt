package com.blockchain.koin.modules

import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.enviroment.EnvironmentUrls
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.data.api.EnvironmentSettings

val environmentModule = module {
    single { EnvironmentSettings() }
        .bind(EnvironmentUrls::class)
        .bind(EnvironmentConfig::class)
}
