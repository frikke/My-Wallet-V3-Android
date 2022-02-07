package com.blockchain.koin.modules

import com.blockchain.koin.dashboardOnboardingFeatureFlag
import com.blockchain.koin.ethMemoHotWalletFeatureFlag
import com.blockchain.koin.fabSheetOrderingFeatureFlag
import com.blockchain.koin.googlePayFeatureFlag
import com.blockchain.koin.landingCtaFeatureFlag
import com.blockchain.koin.pricingQuoteFeatureFlag
import com.blockchain.koin.redesignPart2FeatureFlag
import com.blockchain.koin.ssoSignInPolling
import com.blockchain.koin.stripeAndCheckoutPaymentsFeatureFlag
import com.blockchain.koin.uiTourFeatureFlag
import com.blockchain.koin.unifiedSignInFeatureFlag
import com.blockchain.koin.walletConnectFeatureFlag
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val featureFlagsModule = module {

    factory(ssoSignInPolling) {
        get<RemoteConfig>().featureFlag("android_ff_sso_polling", "Single Sign-on Polling")
    }

    factory(unifiedSignInFeatureFlag) {
        get<RemoteConfig>().featureFlag("android_sso_unified_sign_in", "SSO Unified Sign In")
    }

    single(pricingQuoteFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_new_pricing_quote",
                "New Pricing Quote"
            )
        )
    }.bind(FeatureFlag::class)

    single(stripeAndCheckoutPaymentsFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_checkout_stripe_payments",
                "Checkout And Stripe Payments"
            )
        )
    }.bind(FeatureFlag::class)

    single(landingCtaFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_landing_cta",
                "Landing CTA"
            )
        )
    }.bind(FeatureFlag::class)

    single(dashboardOnboardingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_dashboard_onboarding",
                "Dashboard Onboarding"
            )
        )
    }.bind(FeatureFlag::class)

    single(fabSheetOrderingFeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_fab_buy_cta_on_right",
                "Fab Buy CTA On Right"
            )
        )
    }.bind(FeatureFlag::class)

    single(redesignPart2FeatureFlag) {
        IntegratedFeatureFlag(
            remoteFlag = get<RemoteConfig>().featureFlag(
                "android_ff_redesign_pt2",
                "Wallet Redesign Part 2"
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
}

fun getFeatureFlags(): List<FeatureFlag> {
    return KoinJavaComponent.getKoin().getAll()
}
