package com.blockchain.koin.modules

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.appMaintenanceFeatureFlag
import com.blockchain.koin.appRatingFeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.cachingStoreFeatureFlag
import com.blockchain.koin.customerSupportSheetFeatureFlag
import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.ethMemoHotWalletFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.kycAdditionalInfoFeatureFlag
import com.blockchain.koin.metadataMigrationFeatureFlag
import com.blockchain.koin.newAssetPriceStoreFeatureFlag
import com.blockchain.koin.notificationPreferencesFeatureFlag
import com.blockchain.koin.orderRewardsFeatureFlag
import com.blockchain.koin.referralsFeatureFlag
import com.blockchain.koin.removeSafeconnectFeatureFlag
import com.blockchain.koin.replaceGsonKtxFeatureFlag
import com.blockchain.koin.sendToDomainsAnnouncementFeatureFlag
import com.blockchain.koin.termsAndConditionsFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import io.reactivex.rxjava3.core.Single
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    single(googlePayFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_gpay",
                "Google Pay"
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

    single(kycAdditionalInfoFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_kyc_additional_info",
                "KYC Additional Info"
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

    single(customerSupportSheetFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_customer_support_sheet",
                "Customer Support Sheet"
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

    single(cachingStoreFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_caching_store",
                "Caching Store"
            )
        )
    }.bind(FeatureFlag::class)

    single(newAssetPriceStoreFeatureFlag) {
        IntegratedFeatureFlag(
//            remoteFlag = get<RemoteConfig>().featureFlag(
//                "android_ff_new_asset_price_store",
//                "New AssetPriceStore with Store Cache"
//            )
            // TODO(aromano): Reenable the flag once FiatCryptoInputView and FiatCryptoConversionModel concurrency issues are solved
            remoteFlag = object : FeatureFlag {
                override val key: String = "android_ff_new_asset_price_store"
                override val readableName: String = "New AssetPriceStore with Store Cache"
                override val enabled: Single<Boolean> = Single.just(false)
                override val isEnabled: Boolean = false
            }
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

    single(removeSafeconnectFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_remove_safeconnect_screen",
                "Remove Safeconnect Screen"
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
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
