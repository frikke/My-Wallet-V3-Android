package com.blockchain.koin.modules

import com.blockchain.koin.buyCryptoDashboardButton
import com.blockchain.koin.dynamicAssetsFeatureFlag
import com.blockchain.koin.ssoSignInPolling
import com.blockchain.koin.unifiedSignInFeatureFlag
import com.blockchain.koin.walletRedesignFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.featureflags.DynamicAssetsIntegratedFeatureFlag
import piuk.blockchain.android.featureflags.WalletRedesignIntegratedFeatureFlag

val featureFlagsModule = module {

    factory(ssoSignInPolling) {
        get<RemoteConfig>().featureFlag("android_ff_sso_polling")
    }

    factory(unifiedSignInFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sso_unified_sign_in")
    }

    factory(buyCryptoDashboardButton) {
        get<RemoteConfig>().featureFlag("ff_dashboard_buy_crypto_btn")
    }

    single(dynamicAssetsFeatureFlag) {
        DynamicAssetsIntegratedFeatureFlag(
            gatedFeatures = get(),
            remoteFlag = get<RemoteConfig>().featureFlag("ff_dynamic_asset_enable")
        )
    }.bind(FeatureFlag::class)

    single(walletRedesignFeatureFlag) {
        WalletRedesignIntegratedFeatureFlag(
            gatedFeatures = get(),
            remoteFlag = get<RemoteConfig>().featureFlag("android_ff_wallet_redesign")
        )
    }.bind(FeatureFlag::class)
}
