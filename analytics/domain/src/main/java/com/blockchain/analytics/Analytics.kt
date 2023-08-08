package com.blockchain.analytics

import com.blockchain.analytics.events.LaunchOrigin
import io.reactivex.rxjava3.core.Completable
import java.io.Serializable

interface Analytics {
    fun logEvent(analyticsEvent: AnalyticsEvent)
    fun logEventOnce(analyticsEvent: AnalyticsEvent)
    fun logEventOnceForSession(analyticsEvent: AnalyticsEvent)
}

interface AnalyticsSettings {
    fun flush(): Completable
}

interface UserAnalytics {
    fun logUserProperty(userPropery: UserProperty)
    fun logUserId(userId: String)

    companion object {
        const val KYC_LEVEL = "kyc_level"
        const val KYC_UPDATED_DATE = "kyc_updated_date"
        const val WALLET_ID = "wallet_id"
        const val NABU_USER_ID = "nabu_user_id"
        const val KYC_CREATION_DATE = "kyc_creation_date"
        const val EMAIL_VERIFIED = "email_verified"
        const val TWOFA_ENABLED = "two_fa_enabled"
        const val FUNDED_COINS = "funded_coins"
        const val USD_BALANCE = "usd_balance"
        const val COWBOYS_USER = "cowboys_user"
        const val USER_ELIGIBLE_FOR_EXCHANGE_AWARENESS_PROMPT = "user_eligible_for_prompt"
        const val USER_HAS_SEEN_THE_EXCHANGE_AWARENESS_PROMPT = "user_has_seen_the_prompt"
    }
}

interface AnalyticsEvent {
    val event: String
    val params: Map<String, Serializable>
    val origin: LaunchOrigin?
        get() = null
}

sealed class NotificationAnalytics(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent

object NotificationReceived : NotificationAnalytics("pn_notification_received")
object NotificationAppOpened : NotificationAnalytics("pn_app_opened")

data class UserProperty(val property: String, val value: String) {
    companion object {
        const val MAX_VALUE_LEN = 36
    }
}
