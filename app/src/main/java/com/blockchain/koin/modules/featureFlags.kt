package com.blockchain.koin.modules

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.accountUnificationFeatureFlag
import com.blockchain.koin.appMaintenanceFeatureFlag
import com.blockchain.koin.appRatingFeatureFlag
import com.blockchain.koin.authInterceptorFeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.bindFeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardRejectionCheckFeatureFlag
import com.blockchain.koin.coinWebSocketFeatureFlag
import com.blockchain.koin.cowboysPromoFeatureFlag
import com.blockchain.koin.deeplinkingFeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.metadataMigrationFeatureFlag
import com.blockchain.koin.notificationPreferencesFeatureFlag
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.quickFillButtonsFeatureFlag
import com.blockchain.koin.referralsFeatureFlag
import com.blockchain.koin.sendToDomainsAnnouncementFeatureFlag
import com.blockchain.koin.stxForAllFeatureFlag
import com.blockchain.koin.superAppFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    single(accountUnificationFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_sso_account_unification",
                "SSO Account Unification"
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
                "android_disable_ff_coin_web_socket",
                "Coin Web Socket"
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

    single(superAppFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_new_super_app",
                "Super App mode"
            )
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

    single(bindFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_bind",
                "Enable BIND For LatAm Users"
            )
        )
    }.bind(FeatureFlag::class)

    single(buyRefreshQuoteFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_buy_refresh_quote",
                "Buy Quote refreshing on checkout screen"
            )
        )
    }.bind(FeatureFlag::class)

    single(quickFillButtonsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_quick_fill_buttons",
                "Display Quick Fill buttons in Buy"
            )
        )
    }.bind(FeatureFlag::class)

    single(cardRejectionCheckFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_card_rejection_check",
                "Check Cards for tx rejection"
            )
        )
    }.bind(FeatureFlag::class)

    single(authInterceptorFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_auth_interceptor",
                "Auth Interceptor"
            )
        )
    }.bind(FeatureFlag::class)

    single(cowboysPromoFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_cowboys_promo",
                "Enable Cowboys promotion"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
