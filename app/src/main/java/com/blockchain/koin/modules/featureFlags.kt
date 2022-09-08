package com.blockchain.koin.modules

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.accountUnificationFeatureFlag
import com.blockchain.koin.assetOrderingFeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.bindFeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardPaymentAsyncFeatureFlag
import com.blockchain.koin.cardRejectionCheckFeatureFlag
import com.blockchain.koin.cowboysPromoFeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.evmWithoutL1BalanceFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.loqateFeatureFlag
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.rbFrequencyFeatureFlag
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

    single(intercomChatFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_intercom",
                "Show intercom chat"
            )
        )
    }.bind(FeatureFlag::class)

    single(loqateFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_loqate",
                "Loqate"
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

    single(superAppFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_new_super_app",
                "Super App mode"
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

    single(evmWithoutL1BalanceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_balance_without_l1",
                "Load Balances Without L1 Balance"
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

    single(cardRejectionCheckFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_card_rejection_check",
                "Check Cards for tx rejection"
            )
        )
    }.bind(FeatureFlag::class)

    single(assetOrderingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_brokerage_lists_order",
                "Enable new asset list ordering"
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

    single(cardPaymentAsyncFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "ff_card_payment_async",
                "Enable Async Card Payment"
            )
        )
    }.bind(FeatureFlag::class)

    single(rbFrequencyFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_rb_frequency_suggestion",
                "Enable Recurring Buy suggestion"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
