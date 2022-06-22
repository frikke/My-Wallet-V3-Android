package com.blockchain.koin.modules

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.core.featureflag.LocalOnlyFeatureFlag
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.appMaintenanceFeatureFlag
import com.blockchain.koin.appRatingFeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.coinWebSocketFeatureFlag
import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.metadataMigrationFeatureFlag
import com.blockchain.koin.newAssetPriceStoreFeatureFlag
import com.blockchain.koin.notificationPreferencesFeatureFlag
import com.blockchain.koin.orderRewardsFeatureFlag
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.referralsFeatureFlag
import com.blockchain.koin.replaceGsonKtxFeatureFlag
import com.blockchain.koin.sendToDomainsAnnouncementFeatureFlag
import com.blockchain.koin.speedUpLoginInterestFeatureFlag
import com.blockchain.koin.stxForAllFeatureFlag
import com.blockchain.koin.termsAndConditionsFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    single(speedUpLoginInterestFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_speedup_login_interest",
                "SpeedUp Login - /accounts/savings"
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

    single(replaceGsonKtxFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_replace_gson_ktxjson",
                "Use Kotlinx Serializer (Gson)"
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

    single(deeplinkingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_deeplinking_v2",
                "Deeplinking V2"
            )
        )
    }.bind(FeatureFlag::class)

    single(coinWebSocketFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_coin_web_socket",
                "Coin Web Socket"
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

    single(intercomChatFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_intercom",
                "Show intercom chat"
            )
        )
    }.bind(FeatureFlag::class)

    single(notificationPreferencesFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_notification_preferences_rework",
                "Notification Preferences Rework"
            )
        )
    }.bind(FeatureFlag::class)

    single(newAssetPriceStoreFeatureFlag) {
        LocalOnlyFeatureFlag(
            key = "android_ff_new_asset_price_store",
            readableName = "New AssetPriceStore with Store Cache",
            prefs = get(),
            defaultValue = get<EnvironmentConfig>().isRunningInDebugMode()
        )
    }.bind(FeatureFlag::class)

    single(metadataMigrationFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_account_metadata_migration",
                "Metadata Migration"
            )
        )
    }.bind(FeatureFlag::class)

    single(ethLayerTwoFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_eth_layer_two_networks",
                "Enable Eth L2 Networks"
            )
        )
    }.bind(FeatureFlag::class)

    single(orderRewardsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_order_rewards",
                "Order Rewards Screen By Balance"
            )
        )
    }.bind(FeatureFlag::class)

    single(appMaintenanceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_app_maintenance",
                "App Maintenance"
            )
        )
    }.bind(FeatureFlag::class)

    single(appRatingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_app_rating",
                "App Rating"
            )
        )
    }.bind(FeatureFlag::class)

    single(backupPhraseFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_backup_phrase",
                "Backup Phrase Flow"
            )
        )
    }.bind(FeatureFlag::class)

    single(referralsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_referrals",
                "Referrals"
            )
        )
    }.bind(FeatureFlag::class)

    single(stxForAllFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_stx_all_users",
                "Enable Stacks"
            )
        )
    }.bind(FeatureFlag::class)

    single(plaidFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_plaid",
                "Enable Plaid For ACH Users"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
