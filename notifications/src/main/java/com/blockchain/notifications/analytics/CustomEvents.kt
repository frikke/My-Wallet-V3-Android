package com.blockchain.notifications.analytics

import java.io.Serializable

class PairingEvent(private val pairingMethod: PairingMethod) : AnalyticsEvent {
    override val event: String
        get() = "Wallet Pairing"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Pairing method" to pairingMethod.name
        )
}

enum class PairingMethod(name: String) {
    REVERSE("Reverse")
}

class AppLaunchEvent(private val playServicesFound: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "App Launched"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Play Services found" to playServicesFound
        )
}

class SecondPasswordEvent(private val secondPasswordEnabled: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "Second password event"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Second password enabled" to secondPasswordEnabled
        )
}

class WalletUpgradeEvent(private val successful: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "Wallet Upgraded"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Successful" to successful
        )
}