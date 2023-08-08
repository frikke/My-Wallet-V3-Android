package com.blockchain.koin.modules

import com.blockchain.core.featureflag.CassyAlphaTesterUserTagFeatureFlag
import com.blockchain.core.featureflag.IntegratedFeatureFlag
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import com.blockchain.koin.assetOrderingFeatureFlag
import com.blockchain.koin.bindFeatureFlag
import com.blockchain.koin.blockchainMembershipsFeatureFlag
import com.blockchain.koin.buyIntercomBotFeatureFlag
import com.blockchain.koin.buyRefreshQuoteFeatureFlag
import com.blockchain.koin.cardPaymentAsyncFeatureFlag
import com.blockchain.koin.darkModeFeatureFlag
import com.blockchain.koin.dexFeatureFlag
import com.blockchain.koin.dynamicEthHotWalletAddressFeatureFlag
import com.blockchain.koin.earnTabFeatureFlag
import com.blockchain.koin.exchangeWAPromptFeatureFlag
import com.blockchain.koin.feynmanCheckoutFeatureFlag
import com.blockchain.koin.feynmanEnterAmountFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.googleWalletFeatureFlag
import com.blockchain.koin.improvedPaymentUxFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.iterableAnnouncementsFeatureFlag
import com.blockchain.koin.paymentUxAssetDisplayBalanceFeatureFlag
import com.blockchain.koin.paymentUxTotalDisplayBalanceFeatureFlag
import com.blockchain.koin.plaidFeatureFlag
import com.blockchain.koin.proveFeatureFlag
import com.blockchain.koin.rbExperimentFeatureFlag
import com.blockchain.koin.sellSwapBrokerageQuoteFeatureFlag
import com.blockchain.koin.stakingWithdrawalsFeatureFlag
import com.blockchain.koin.topMoversInBuy
import com.blockchain.koin.vgsFeatureFlag
import com.blockchain.koin.walletConnectV1FeatureFlag
import com.blockchain.koin.walletConnectV2FeatureFlag
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

    single(plaidFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_plaid",
                "Enable Plaid For ACH Users"
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

    single(dexFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_dex",
                "Dex"
            )
        )
    }.bind(FeatureFlag::class)

    single(proveFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_provedotcom",
                "Prove.com"
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

    single(cardPaymentAsyncFeatureFlag) {
        CassyAlphaTesterUserTagFeatureFlag(
            IntegratedFeatureFlag(
                remoteFlag = get<RemoteConfigService>().featureFlag(
                    "ff_card_payment_async",
                    "Enable Async Card Payment"
                )
            )
        )
    }.bind(FeatureFlag::class)

    single(vgsFeatureFlag) {
        CassyAlphaTesterUserTagFeatureFlag(
            IntegratedFeatureFlag(
                remoteFlag = get<RemoteConfigService>().featureFlag(
                    "android_ff_vgs",
                    "Enable VGS"
                )
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

    single(exchangeWAPromptFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "exchange_wa_prompt",
                "Exchange WA prompt"
            )
        )
    }.bind(FeatureFlag::class)

    single(sellSwapBrokerageQuoteFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_sell_swap_brokerage_quote",
                "Sell/Swap Brokerage Quote"
            )
        )
    }.bind(FeatureFlag::class)

    single(activeRewardsWithdrawalsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_active_rewards_withdrawals",
                "Active Rewards Withdrawals"
            )
        )
    }.bind(FeatureFlag::class)

    single(stakingWithdrawalsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_staking_withdrawals",
                "Staking Withdrawals"
            )
        )
    }.bind(FeatureFlag::class)

    single(iterableAnnouncementsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_iterable_announcements",
                "Iterable Announcements"
            )
        )
    }.bind(FeatureFlag::class)

    single(dynamicEthHotWalletAddressFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_dynamic_eth_hot_wallet_address",
                "Dynamic ETH HWS Address"
            )
        )
    }.bind(FeatureFlag::class)

    single(topMoversInBuy) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "blockchain_app_configuration_buy_top_movers_is_enabled",
                "Show Top Movers in Buy flow"
            )
        )
    }.bind(FeatureFlag::class)

    single(buyIntercomBotFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "blockchain_app_configuration_buy_intercom_bot",
                "intercom bot in buy flow"
            )
        )
    }.bind(FeatureFlag::class)

    single(walletConnectV2FeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_walletconnect_v2",
                "Enable WalletConnect V2"
            )
        )
    }.bind(FeatureFlag::class)

    single(walletConnectV1FeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "android_ff_walletconnect_v1",
                "Enable WalletConnect V1"
            )
        )
    }.bind(FeatureFlag::class)

    single(darkModeFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfigService>().featureFlag(
                "blockchain_app_configuration_and_dark_mode",
                "dark mode"
            )
        )
    }.bind(FeatureFlag::class)
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
