package com.blockchain.koin.modules

import com.blockchain.koin.dynamicAssetsFeatureFlag
import com.blockchain.koin.ssoSignInPolling
import com.blockchain.koin.unifiedSignInFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.featureflags.DynamicAssetsIntegratedFeatureFlag

val featureFlagsModule = module {

    factory(ssoSignInPolling) {
        get<RemoteConfig>().featureFlag("android_ff_sso_polling")
    }

    factory(unifiedSignInFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sso_unified_sign_in")
    }

    single(dynamicAssetsFeatureFlag) {
        DynamicAssetsIntegratedFeatureFlag(
            gatedFeatures = get(),
            remoteFlag = get<RemoteConfig>().featureFlag("ff_dynamic_asset_enable")
        )
    }.bind(FeatureFlag::class)
}
