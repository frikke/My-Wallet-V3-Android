package com.blockchain.analytics.events

import com.blockchain.analytics.AnalyticsEvent
import java.io.Serializable

@Deprecated("Analytics events should be defined near point of use")
class PairingEvent(private val pairingMethod: PairingMethod) : AnalyticsEvent {
    override val event: String
        get() = "Wallet Pairing"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Pairing method" to pairingMethod.name
        )
}

@Deprecated("Analytics events should be defined near point of use")
enum class PairingMethod(name: String) {
    REVERSE("Reverse")
}

@Deprecated("Analytics events should be defined near point of use")
class AppLaunchEvent(private val playServicesFound: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "App Launched"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Play Services found" to playServicesFound
        )
}

@Deprecated("Analytics events should be defined near point of use")
class SecondPasswordEvent(private val secondPasswordEnabled: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "Second password event"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Second password enabled" to secondPasswordEnabled
        )
}

@Deprecated("Analytics events should be defined near point of use")
class WalletUpgradeEvent(private val successful: Boolean) : AnalyticsEvent {
    override val event: String
        get() = "Wallet Upgraded"
    override val params: Map<String, Serializable>
        get() = mapOf(
            "Successful" to successful
        )
}
