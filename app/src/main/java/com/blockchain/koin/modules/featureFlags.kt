package com.blockchain.koin.modules

import com.blockchain.koin.achDepositWithdrawFeatureFlag
import com.blockchain.koin.fullScreenTxFlowFeatureFlag
import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.mwaFeatureFlag
import com.blockchain.koin.obFeatureFlag
import com.blockchain.koin.sddFeatureFlag
import com.blockchain.koin.ssoAccountRecoveryFeatureFlag
import com.blockchain.koin.unifiedSignInFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.module

val featureFlagsModule = module {

    factory(interestAccountFeatureFlag) {
        get<RemoteConfig>().featureFlag("interest_account_enabled")
    }

    factory(obFeatureFlag) {
        get<RemoteConfig>().featureFlag("ob_enabled")
    }

    factory(achDepositWithdrawFeatureFlag) {
        get<RemoteConfig>().featureFlag("ach_deposit_withdrawal_enabled")
    }

    factory(sddFeatureFlag) {
        get<RemoteConfig>().featureFlag("sdd_enabled")
    }

    factory(mwaFeatureFlag) {
        get<RemoteConfig>().featureFlag("mwa_enabled")
    }

    factory(ssoAccountRecoveryFeatureFlag) {
        get<RemoteConfig>().featureFlag("sso_account_recovery_enabled")
    }

    factory(fullScreenTxFlowFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_full_screen_tx_flow")
    }

    factory(unifiedSignInFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sso_unified_sign_in")
    }
}
