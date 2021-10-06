package com.blockchain.koin.modules

import com.blockchain.koin.achDepositWithdrawFeatureFlag
import com.blockchain.koin.obFeatureFlag
import com.blockchain.koin.sddFeatureFlag
import com.blockchain.koin.unifiedSignInFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.module

val featureFlagsModule = module {

    factory(obFeatureFlag) {
        get<RemoteConfig>().featureFlag("ob_enabled")
    }

    factory(achDepositWithdrawFeatureFlag) {
        get<RemoteConfig>().featureFlag("ach_deposit_withdrawal_enabled")
    }

    factory(sddFeatureFlag) {
        get<RemoteConfig>().featureFlag("sdd_enabled")
    }

    factory(unifiedSignInFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sso_unified_sign_in")
    }
}
