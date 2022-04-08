package com.blockchain.koin.modules

import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.customerSupportSheetFeatureFlag
import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.disableMoshiSerializerFeatureFlag
import com.blockchain.koin.embraceFeatureFlag
import com.blockchain.koin.enableKotlinSerializerFeatureFlag
import com.blockchain.koin.entitySwitchSilverEligibilityFeatureFlag
import com.blockchain.koin.ethMemoHotWalletFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.kycAdditionalInfoFeatureFlag
import com.blockchain.koin.notificationPreferencesFeatureFlag
import com.blockchain.koin.redesignPart2CoinViewFeatureFlag
import com.blockchain.koin.replaceGsonKtxFeatureFlag
import com.blockchain.koin.sendToDomainsAnnouncementFeatureFlag
import com.blockchain.koin.termsAndConditionsFeatureFlag
import com.blockchain.koin.uiTourFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

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

    single(enableKotlinSerializerFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_enable_kotlin_serializer",
                "Use Kotlinx Serializer (Jackson)"
            )
        )
    }.bind(FeatureFlag::class)

    single(disableMoshiSerializerFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_disable_moshi",
                "Use Kotlinx Serializer (Moshi)"
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

    single(entitySwitchSilverEligibilityFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_entity_switch_silver_eligibility",
                "Entity Switch Silver Eligibility"
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

    single(embraceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_embrace",
                "Embrace.io"
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
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
