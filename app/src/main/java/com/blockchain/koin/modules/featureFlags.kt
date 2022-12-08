package com.blockchain.koin.modules

import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.assetOrderingFeatureFlag
import com.blockchain.koin.backupPhraseFeatureFlag
import com.blockchain.koin.bindFeatureFlag
import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.blockchainMembershipsFeatureFlag
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardPaymentAsyncFeatureFlag
import com.blockchain.koin.coinNetworksFeatureFlag
import com.blockchain.koin.cowboysPromoFeatureFlag
import com.blockchain.koin.earnTabFeatureFlag
import com.blockchain.koin.ethLayerTwoFeatureFlag
import com.blockchain.koin.evmWithoutL1BalanceFeatureFlag
import com.blockchain.koin.feynmanCheckoutFeatureFlag
import com.blockchain.koin.feynmanEnterAmountFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.googleWalletFeatureFlag
import com.blockchain.koin.hideDustFeatureFlag
import com.blockchain.koin.improvedPaymentUxFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.paymentUxAssetDisplayBalanceFeatureFlag
import com.blockchain.koin.paymentUxTotalDisplayBalanceFeatureFlag
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.rbExperimentFeatureFlag
import com.blockchain.koin.rbFrequencyFeatureFlag
import com.blockchain.koin.sardineFeatureFlag
import com.blockchain.koin.sessionIdFeatureFlag
import com.blockchain.koin.stakingAccountFeatureFlag
import com.blockchain.koin.stxForAllFeatureFlag
import com.blockchain.koin.superAppFeatureFlag
import com.blockchain.koin.superappRedesignFeatureFlag
import com.blockchain.koin.unifiedBalancesFlag
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    single(googlePayFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_gpay",
                "Google Pay"
            )
        )
    }.bind(FeatureFlag::class)

    single(blockchainCardFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_blockchain_card",
                "Blockchain Card"
            )
        )
    }.bind(FeatureFlag::class)

    single(intercomChatFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_intercom",
                "Show intercom chat"
            )
        )
    }.bind(FeatureFlag::class)

    single(buyRefreshQuoteFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_buy_refresh_quote",
                "Buy Quote refreshing on checkout screen"
            )
        )
    }.bind(FeatureFlag::class)

    single(superAppFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_new_super_app",
                "Super App mode"
            )
        )
    }.bind(FeatureFlag::class)

    single(ethLayerTwoFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_eth_layer_two_networks",
                "Enable Eth L2 Networks"
            )
        )
    }.bind(FeatureFlag::class)

    single(coinNetworksFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_coin_networks",
                "Enable Coin Networks"
            )
        )
    }.bind(FeatureFlag::class)

    single(evmWithoutL1BalanceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_balance_without_l1",
                "Load Balances Without L1 Balance"
            )
        )
    }.bind(FeatureFlag::class)

    single(backupPhraseFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_backup_phrase",
                "Backup Phrase Flow"
            )
        )
    }.bind(FeatureFlag::class)

    single(stxForAllFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_stx_all_users",
                "Enable Stacks"
            )
        )
    }.bind(FeatureFlag::class)

    single(plaidFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_plaid",
                "Enable Plaid For ACH Users"
            )
        )
    }.bind(FeatureFlag::class)

    single(unifiedBalancesFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_unified_balances_ff",
                "Enable Balances from unified balances endpoint"
            )
        )
    }.bind(FeatureFlag::class)

    single(bindFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_bind",
                "Enable BIND For LatAm Users"
            )
        )
    }.bind(FeatureFlag::class)

    single(assetOrderingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_brokerage_lists_order",
                "Enable new asset list ordering"
            )
        )
    }.bind(FeatureFlag::class)

    single(cowboysPromoFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_cowboys_promo",
                "Enable Cowboys promotion"
            )
        )
    }.bind(FeatureFlag::class)

    single(cardPaymentAsyncFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "ff_card_payment_async",
                "Enable Async Card Payment"
            )
        )
    }.bind(FeatureFlag::class)

    single(rbFrequencyFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_rb_frequency_suggestion",
                "Enable Recurring Buy suggestion"
            )
        )
    }.bind(FeatureFlag::class)

    single(feynmanEnterAmountFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_enter_amount_screen_ff_feynman",
                "Feynman crypto enter amount screen"
            )
        )
    }.bind(FeatureFlag::class)

    single(feynmanCheckoutFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_checkout_screen_ff_feynman",
                "Feynman checkout screen Create + Confirm at once"
            )
        )
    }.bind(FeatureFlag::class)

    single(rbExperimentFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_recurring_buy_frequency_experiment",
                "Recurring Buy Experiment"
            )
        )
    }.bind(FeatureFlag::class)

    single(superappRedesignFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_superapp_redesign",
                "Enable SuperApp Redesign"
            )
        )
    }.bind(FeatureFlag::class)

    single(sessionIdFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "ff_x_session_id",
                "Send X-Session-ID Header"
            )
        )
    }.bind(FeatureFlag::class)

    single(sardineFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "ff_sardine",
                "Enable Sardine"
            )
        )
    }.bind(FeatureFlag::class)

    single(stakingAccountFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_staking_account",
                "Enable Staking Account & New Coinview"
            )
        )
    }.bind(FeatureFlag::class)

    single(paymentUxTotalDisplayBalanceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_payment_ux_total_display_balance",
                "Enable Payment UX Dashboard Total display balance"
            )
        )
    }.bind(FeatureFlag::class)

    single(paymentUxAssetDisplayBalanceFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_payment_ux_asset_display_balance",
                "Enable Payment UX Dashboard Asset display balance"
            )
        )
    }.bind(FeatureFlag::class)

    single(hideDustFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_hide_dust",
                "Enable Hiding Dust"
            )
        )
    }.bind(FeatureFlag::class)

    single(googleWalletFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_google_wallet",
                "Google Wallet"
            )
        )
    }.bind(FeatureFlag::class)

    single(blockchainMembershipsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_memberships",
                "Blockchain.com Memberships"
            )
        )
    }.bind(FeatureFlag::class)

    single(improvedPaymentUxFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_improved_payment_ux",
                "Improved Payment UX"
            )
        )
    }.bind(FeatureFlag::class)

    single(earnTabFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_nav_bar_earn",
                "Earn on Bottom Nav Bar"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
