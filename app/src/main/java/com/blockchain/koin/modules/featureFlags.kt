package com.blockchain.koin.modules

import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.ethMemoHotWalletFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.redesignPart2CoinViewFeatureFlag
import com.blockchain.koin.redesignPart2FeatureFlag
import com.blockchain.koin.sendToDomainsAnnouncementFeatureFlag
import com.blockchain.koin.termsAndConditionsFeatureFlag
import com.blockchain.koin.uiTourFeatureFlag
import com.blockchain.koin.walletConnectFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    single(redesignPart2FeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_redesign_pt2",
                "Wallet Redesign Part 2"
            )
        )
    }.bind(FeatureFlag::class)

    single(redesignPart2CoinViewFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_redesign_pt2_coinview",
                "Wallet Redesign Coinview"
            )
        )
    }.bind(FeatureFlag::class)

    single(googlePayFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_gpay",
                "Google Pay"
            )
        )
    }.bind(FeatureFlag::class)

    single(walletConnectFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_wallet_connect",
                "Wallet connect"
            )
        )
    }.bind(FeatureFlag::class)

    single(uiTourFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_ui_tour",
                "Ui Tour"
            )
        )
    }.bind(FeatureFlag::class)

    single(ethMemoHotWalletFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_eth_memo",
                "ETH Memo for Hot Wallets"
            )
        )
    }.bind(FeatureFlag::class)

    single(blockchainCardFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_blockchain_card",
                "Blockchain Card"
            )
        )
    }.bind(FeatureFlag::class)

    single(termsAndConditionsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_terms_and_conditions",
                "Terms and Conditions"
            )
        )
    }.bind(FeatureFlag::class)

    single(sendToDomainsAnnouncementFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_send_to_domains_announcement",
                "Show Send To Domains Banner"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
